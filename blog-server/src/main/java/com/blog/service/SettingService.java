package com.blog.service;

import com.blog.dto.SettingDTO;
import com.blog.dto.SiteInfoResponse;

import java.util.List;

public interface SettingService {

    // Admin methods
    List<SettingDTO> getAllSettings();

    void updateSettings(List<SettingDTO> settings);

    // Web methods
    SiteInfoResponse getSiteInfo();
}
