package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.LinkDTO;
import com.blog.service.LinkService;
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
 * 友链管理控制器
 */
@RestController("adminLinkController")
@RequestMapping("/api/admin/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    /**
     * 获取所有友链列表
     */
    @GetMapping
    public Result<List<LinkDTO>> list() {
        List<LinkDTO> list = linkService.getAllLinks();
        return Result.success(list);
    }

    /**
     * 创建友链
     *
     * @param dto 友链信息
     * @return 新建友链的 ID
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody LinkDTO dto) {
        Long id = linkService.createLink(dto);
        return Result.success(id);
    }

    /**
     * 更新友链
     *
     * @param id  友链 ID
     * @param dto 友链信息
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LinkDTO dto) {
        linkService.updateLink(id, dto);
        return Result.success();
    }

    /**
     * 删除友链
     *
     * @param id 友链 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        linkService.deleteLink(id);
        return Result.success();
    }
}
