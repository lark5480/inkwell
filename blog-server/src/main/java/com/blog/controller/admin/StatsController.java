package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.StatsOverviewResponse;
import com.blog.service.SearchPerfCollector;
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
    private final SearchPerfCollector searchPerfCollector;

    /** 获取后台统计概览数据 */
    @GetMapping("/overview")
    public Result<StatsOverviewResponse> overview() {
        StatsOverviewResponse dto = statsService.getOverview();
        return Result.success(dto);
    }

    /** 获取搜索性能统计（QPS、缓存命中率、P99 延迟） */
    @GetMapping("/search-perf")
    public Result<SearchPerfCollector.SearchPerfStats> getSearchPerfStats() {
        return Result.success(searchPerfCollector.getStats());
    }
}
