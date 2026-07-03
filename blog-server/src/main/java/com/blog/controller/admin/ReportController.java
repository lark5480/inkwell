package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.PageDTO;
import com.blog.entity.CommentReport;
import com.blog.service.CommentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 举报管理控制器
 */
@RestController("adminReportController")
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final CommentReportService commentReportService;

    /**
     * 分页查询举报列表
     *
     * @param page     页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param status   举报状态（可选），按状态筛选
     * @return 分页举报数据
     */
    @GetMapping
    public Result<PageDTO<CommentReport>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        PageDTO<CommentReport> result = commentReportService.getReportPage(page, pageSize, status);
        return Result.success(result);
    }

    /**
     * 更新举报处理状态
     *
     * @param id   举报 ID
     * @param body 请求体，包含 status 字段（如 "resolved"、"dismissed"）
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        commentReportService.updateReportStatus(id, status);
        return Result.success();
    }
}
