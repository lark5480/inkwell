package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.TagDTO;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("webTagController")
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/tags")
    public Result<List<TagDTO>> list() {
        List<TagDTO> list = tagService.getTags();
        return Result.success(list);
    }
}
