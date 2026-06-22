package com.blog.service;

import java.util.List;

public interface UserBlockService {

    /**
     * 拉黑用户
     */
    void block(Long blockerId, Long blockedId);

    /**
     * 取消拉黑
     */
    void unblock(Long blockerId, Long blockedId);

    /**
     * 查询指定用户已拉黑的用户ID列表
     */
    List<Long> getBlockedUserIds(Long blockerId);

    /**
     * 检查是否已拉黑
     */
    boolean isBlocked(Long blockerId, Long blockedId);
}
