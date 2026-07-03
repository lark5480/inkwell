package com.blog.service;

import com.blog.dto.StatsOverviewResponse;

/**
 * 统计相关业务接口
 */
public interface StatsService {

    /**
     * 获取后台统计概览
     *
     * @return 统计概览数据（文章数、评论数、浏览量、分类数、标签数、友链数）
     */
    StatsOverviewResponse getOverview();
}
