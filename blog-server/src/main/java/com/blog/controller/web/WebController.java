package com.blog.controller.web;

import com.blog.dto.PageDTO;
import com.blog.common.Result;
import com.blog.dto.ArchiveItem;
import com.blog.dto.ArticleWebResponse;
import com.blog.dto.SiteInfoResponse;
import com.blog.service.ArticleService;
import com.blog.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class WebController {

    private final ArticleService articleService;
    private final SettingService settingService;

    @GetMapping("/search")
    public Result<PageDTO<ArticleWebResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageDTO<ArticleWebResponse> result = articleService.searchArticles(q, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/site-info")
    public Result<SiteInfoResponse> siteInfo() {
        SiteInfoResponse dto = settingService.getSiteInfo();
        return Result.success(dto);
    }

    @GetMapping("/archives")
    public Result<List<ArchiveItem>> archives() {
        List<ArchiveItem> list = articleService.getArchives();
        return Result.success(list);
    }

    @GetMapping(value = "/rss.xml", produces = "application/rss+xml")
    public String rss() {
        return articleService.generateRss();
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap() {
        return articleService.generateSitemap();
    }
}
