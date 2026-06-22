package com.blog.service.impl;

import com.blog.dto.BlockedUserResponse;
import com.blog.entity.User;
import com.blog.entity.UserBlock;
import com.blog.exception.BusinessException;
import com.blog.repository.UserBlockRepository;
import com.blog.repository.UserRepository;
import com.blog.service.UserBlockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserBlockServiceImpl implements UserBlockService {

    private static final Logger log = LoggerFactory.getLogger(UserBlockServiceImpl.class);

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    public UserBlockServiceImpl(UserBlockRepository userBlockRepository,
                                UserRepository userRepository) {
        this.userBlockRepository = userBlockRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void block(Long blockerId, Long blockedId) {
        log.info("拉黑 blockerId={} blockedId={}", blockerId, blockedId);

        if (blockerId.equals(blockedId)) {
            throw new BusinessException(400, "Cannot block yourself");
        }

        if (!userRepository.existsById(blockedId)) {
            throw new BusinessException(404, "User not found");
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BusinessException(400, "Already blocked");
        }

        UserBlock userBlock = UserBlock.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();
        userBlockRepository.save(userBlock);
    }

    @Override
    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        log.info("取消拉黑 blockerId={} blockedId={}", blockerId, blockedId);

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BusinessException(400, "Not blocked");
        }

        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getBlockedUserIds(Long blockerId) {
        List<Long> ids = userBlockRepository.findBlockedUserIdsByBlockerId(blockerId);
        return ids != null ? ids : Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers(Long blockerId) {
        List<Long> ids = getBlockedUserIds(blockerId);
        if (ids.isEmpty()) return Collections.emptyList();
        return userRepository.findAllById(ids).stream()
                .map(u -> new BlockedUserResponse(
                        u.getId(), u.getNickname(), u.getAvatar(), u.getBio()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }
}
