package com.blog.service;

import com.blog.dto.*;

/**
 * 关注/粉丝业务接口
 */
public interface FollowService {

    /**
     * 切换关注状态（关注/取消关注）
     *
     * @param currentUserId 当前操作用户ID
     * @param targetUserId  目标用户ID
     * @return 关注切换结果
     */
    FollowToggleResponse toggleFollow(Long currentUserId, Long targetUserId);

    /**
     * 获取用户的粉丝列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 粉丝列表分页结果
     */
    PageDTO<FollowerResponse> getFollowers(Long userId, int page, int pageSize);

    /**
     * 获取用户的关注列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 关注列表分页结果
     */
    PageDTO<FollowingResponse> getFollowing(Long userId, int page, int pageSize);

    /**
     * 判断当前用户是否已关注目标用户
     *
     * @param currentUserId 当前用户ID
     * @param targetUserId  目标用户ID
     * @return true 已关注，false 未关注
     */
    boolean isFollowing(Long currentUserId, Long targetUserId);
}
