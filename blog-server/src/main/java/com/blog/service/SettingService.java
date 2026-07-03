package com.blog.service;

import com.blog.dto.SettingDTO;
import com.blog.dto.SiteInfoResponse;

import java.util.List;

/**
 * 博客系统设置服务接口
 */
public interface SettingService {

    // Admin methods
    /**
     * 获取所有系统设置
     *
     * @return 系统设置项列表
     */
    List<SettingDTO> getAllSettings();

    /**
     * 批量更新系统设置
     *
     * @param settings 设置项列表
     */
    void updateSettings(List<SettingDTO> settings);

    // Web methods
    /**
     * 获取站点公开信息（前端展示用）
     *
     * @return 站点信息响应
     */
    SiteInfoResponse getSiteInfo();
}
