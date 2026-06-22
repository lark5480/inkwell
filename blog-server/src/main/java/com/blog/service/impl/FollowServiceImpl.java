package com.blog.service.impl;

import com.blog.dto.*;
import com.blog.entity.User;
import com.blog.entity.UserFollow;
import com.blog.exception.BusinessException;
import com.blog.repository.UserFollowRepository;
import com.blog.repository.UserRepository;
import com.blog.service.FollowService;
import com.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FollowToggleResponse toggleFollow(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(400, "Cannot follow yourself");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(404, "User not found"));

        boolean alreadyFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(
                currentUserId, targetUserId);

        if (alreadyFollowing) {
            userFollowRepository.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
            target.setFollowerCount(Math.max(0, target.getFollowerCount() - 1));
            userRepository.save(target);

            User follower = userRepository.findById(currentUserId).orElse(null);
            if (follower != null) {
                follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
                userRepository.save(follower);
            }

            log.info("取消关注 followerId={} followingId={}", currentUserId, targetUserId);
            return new FollowToggleResponse(false, target.getFollowerCount(),
                    follower != null ? follower.getFollowingCount() : 0);
        } else {
            userFollowRepository.save(UserFollow.builder()
                    .followerId(currentUserId)
                    .followingId(targetUserId)
                    .build());
            target.setFollowerCount(target.getFollowerCount() + 1);
            userRepository.save(target);

            User follower = userRepository.findById(currentUserId).orElse(null);
            if (follower != null) {
                follower.setFollowingCount(follower.getFollowingCount() + 1);
                userRepository.save(follower);
            }

            log.info("关注成功 followerId={} followingId={}", currentUserId, targetUserId);

            /* 发送关注通知 */
            notificationService.notifyFollow(currentUserId, targetUserId);

            return new FollowToggleResponse(true, target.getFollowerCount(),
                    follower != null ? follower.getFollowingCount() : 0);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<FollowerResponse> getFollowers(Long userId, int page, int pageSize) {
        Page<UserFollow> followsPage = userFollowRepository
                .findByFollowingIdOrderByCreateTimeDesc(userId, PageRequest.of(page - 1, pageSize));

        List<FollowerResponse> records = followsPage.getContent().stream()
                .map(f -> {
                    User user = userRepository.findById(f.getFollowerId()).orElse(null);
                    if (user == null) {
                        return null;
                    }
                    return new FollowerResponse(
                            user.getId(), user.getNickname(), user.getAvatar(),
                            user.getBio(), f.getCreateTime());
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageDTO<>(records, followsPage.getTotalElements(), page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<FollowingResponse> getFollowing(Long userId, int page, int pageSize) {
        Page<UserFollow> followsPage = userFollowRepository
                .findByFollowerIdOrderByCreateTimeDesc(userId, PageRequest.of(page - 1, pageSize));

        List<FollowingResponse> records = followsPage.getContent().stream()
                .map(f -> {
                    User user = userRepository.findById(f.getFollowingId()).orElse(null);
                    if (user == null) {
                        return null;
                    }
                    return new FollowingResponse(
                            user.getId(), user.getNickname(), user.getAvatar(),
                            user.getBio(), f.getCreateTime());
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageDTO<>(records, followsPage.getTotalElements(), page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long currentUserId, Long targetUserId) {
        if (currentUserId == null) {
            return false;
        }
        return userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
    }
}
