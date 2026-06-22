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

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {
        
    private final UserRepository userRepository;

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

    @GetMapping("/{id}")
    public Result<UserAdminResponse> getById(@PathVariable Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        return Result.success(new UserAdminResponse(
                u.getId(), u.getUsername(), u.getNickname(),
                u.getEmail(), u.getAvatar(), u.getRole(),
                u.getStatus(), u.getCreateTime()));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        u.setStatus(body.get("status"));
        userRepository.save(u);
        return Result.success();
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        u.setRole(body.get("role"));
        userRepository.save(u);
        return Result.success();
    }
}
