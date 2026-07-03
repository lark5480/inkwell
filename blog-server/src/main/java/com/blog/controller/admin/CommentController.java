package com.blog.controller.admin;

import com.blog.dto.PageDTO;
import com.blog.common.Result;
import com.blog.dto.CommentAdminResponse;
import com.blog.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 评论管理控制器
 */
@RestController("adminCommentController")
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 分页查询评论列表
     *
     * @param page     页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param status   评论状态筛选（可选）
     * @return 分页评论数据
     */
    @GetMapping
    public Result<PageDTO<CommentAdminResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        PageDTO<CommentAdminResponse> result = commentService.getCommentPage(page, pageSize, status);
        return Result.success(result);
    }

    /**
     * 更新评论状态（审核通过/驳回等）
     *
     * @param id   评论 ID
     * @param body 请求体，包含 status 字段
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        commentService.updateCommentStatus(id, status);
        return Result.success();
    }

    /**
     * 删除指定评论
     *
     * @param id 评论 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }
}
