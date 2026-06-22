package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.LinkDTO;
import com.blog.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("webLinkController")
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @GetMapping("/links")
    public Result<List<LinkDTO>> list() {
        List<LinkDTO> list = linkService.getLinks();
        return Result.success(list);
    }
}
