package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.TagDTO;
import com.blog.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签管理控制器
 */
@RestController("adminTagController")
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 获取所有标签列表
     */
    @GetMapping
    public Result<List<TagDTO>> list() {
        List<TagDTO> list = tagService.getAllTags();
        return Result.success(list);
    }

    /**
     * 创建标签
     *
     * @param dto 标签数据
     * @return 新标签的 ID
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TagDTO dto) {
        Long id = tagService.createTag(dto);
        return Result.success(id);
    }

    /**
     * 更新标签
     *
     * @param id  标签 ID
     * @param dto 标签数据
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        tagService.updateTag(id, dto);
        return Result.success();
    }

    /**
     * 删除标签
     *
     * @param id 标签 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }
}
