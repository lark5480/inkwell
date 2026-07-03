package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.TagDTO;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tag 控制器 - 提供标签相关的前端 API
 */
@RestController("webTagController")
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 获取所有标签列表
     *
     * @return 标签列表，每个标签包含 id、名称等信息
     */
    @GetMapping("/tags")
    public Result<List<TagDTO>> list() {
        List<TagDTO> list = tagService.getTags();
        return Result.success(list);
    }
}
