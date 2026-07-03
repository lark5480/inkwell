package com.blog.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.Result;
import com.blog.dto.SettingDTO;
import com.blog.service.SettingService;

import lombok.RequiredArgsConstructor;

/**
 * 系统设置控制器
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SettingController {
    
    private final SettingService settingService;

    /**
     * 获取所有系统设置
     *
     * @return 系统设置列表
     */
    @GetMapping
    public Result<List<SettingDTO>> getAll() {
        List<SettingDTO> list = settingService.getAllSettings();
        return Result.success(list);
    }

    /**
     * 批量更新系统设置
     *
     * @param settings 设置键值对集合
     * @return 无返回数据
     */
    @PutMapping
    public Result<Void> updateAll(@RequestBody Map<String, String> settings) {
        List<SettingDTO> list = settings.entrySet().stream()
                .map(entry -> new SettingDTO(null, entry.getKey(), entry.getValue(), null))
                .toList();
        settingService.updateSettings(list);
        return Result.success();
    }
}
