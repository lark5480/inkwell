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

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SettingController {
    
    private final SettingService settingService;

    @GetMapping
    public Result<List<SettingDTO>> getAll() {
        List<SettingDTO> list = settingService.getAllSettings();
        return Result.success(list);
    }

    @PutMapping
    public Result<Void> updateAll(@RequestBody Map<String, String> settings) {
        List<SettingDTO> list = settings.entrySet().stream()
                .map(entry -> new SettingDTO(null, entry.getKey(), entry.getValue(), null))
                .toList();
        settingService.updateSettings(list);
        return Result.success();
    }
}
