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

/**
 * Web前端控制器，提供博客前台页面所需的API接口
 */
@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class WebController {

    private final ArticleService articleService;
    private final SettingService settingService;

    /**
     * 搜索文章
     *
     * @param q        搜索关键词
     * @param page     页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 分页搜索结果
     */
    @GetMapping("/search")
    public Result<PageDTO<ArticleWebResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageDTO<ArticleWebResponse> result = articleService.searchArticles(q, page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取站点信息（站点名称、描述、Logo 等）
     *
     * @return 站点信息
     */
    @GetMapping("/site-info")
    public Result<SiteInfoResponse> siteInfo() {
        SiteInfoResponse dto = settingService.getSiteInfo();
        return Result.success(dto);
    }

    /**
     * 获取文章归档列表（按年月分组）
     *
     * @return 归档列表
     */
    @GetMapping("/archives")
    public Result<List<ArchiveItem>> archives() {
        List<ArchiveItem> list = articleService.getArchives();
        return Result.success(list);
    }

    /**
     * 生成 RSS 订阅 XML
     *
     * @return RSS XML 字符串
     */
    @GetMapping(value = "/rss.xml", produces = "application/rss+xml")
    public String rss() {
        return articleService.generateRss();
    }

    /**
     * 生成 Sitemap XML（SEO 站点地图）
     *
     * @return Sitemap XML 字符串
     */
    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap() {
        return articleService.generateSitemap();
    }
}
