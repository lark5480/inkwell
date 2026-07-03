package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.StatsOverviewResponse;
import com.blog.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台统计控制器 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 获取后台统计概览数据 */
    @GetMapping("/overview")
    public Result<StatsOverviewResponse> overview() {
        StatsOverviewResponse dto = statsService.getOverview();
        return Result.success(dto);
    }
}
