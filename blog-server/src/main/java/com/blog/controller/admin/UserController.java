package com.blog.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.Result;
import com.blog.dto.UserAdminResponse;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {
        
    private final UserRepository userRepository;

    /**
     * 获取所有用户列表
     *
     * @return 用户列表（包含 id、用户名、昵称、邮箱、头像、角色、状态、创建时间）
     */
    @GetMapping
    public Result<List<UserAdminResponse>> list() {
        List<User> users = userRepository.findAll();
        List<UserAdminResponse> list = users.stream()
                .map(u -> new UserAdminResponse(
                        u.getId(), u.getUsername(), u.getNickname(),
                        u.getEmail(), u.getAvatar(), u.getRole(),
                        u.getStatus(), u.getCreateTime()))
                .toList();
        return Result.success(list);
    }

    /**
     * 根据 ID 获取单个用户信息
     *
     * @param id 用户 ID
     * @return 用户详细信息
     */
    @GetMapping("/{id}")
    public Result<UserAdminResponse> getById(@PathVariable Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        return Result.success(new UserAdminResponse(
                u.getId(), u.getUsername(), u.getNickname(),
                u.getEmail(), u.getAvatar(), u.getRole(),
                u.getStatus(), u.getCreateTime()));
    }

    /**
     * 更新用户状态（启用/禁用）
     *
     * @param id   用户 ID
     * @param body 请求体，包含 status 字段（1=启用，0=禁用）
     * @return 无内容
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        u.setStatus(body.get("status"));
        userRepository.save(u);
        return Result.success();
    }

    /**
     * 更新用户角色
     *
     * @param id   用户 ID
     * @param body 请求体，包含 role 字段（如 ADMIN、USER）
     * @return 无内容
     */
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        u.setRole(body.get("role"));
        userRepository.save(u);
        return Result.success();
    }
}
