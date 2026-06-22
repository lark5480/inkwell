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

@RestController("adminReportController")
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final CommentReportService commentReportService;

    @GetMapping
    public Result<PageDTO<CommentReport>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        PageDTO<CommentReport> result = commentReportService.getReportPage(page, pageSize, status);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        commentReportService.updateReportStatus(id, status);
        return Result.success();
    }
}
