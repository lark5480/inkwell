package com.blog.service;

import com.blog.dto.*;

public interface FollowService {

    FollowToggleResponse toggleFollow(Long currentUserId, Long targetUserId);

    PageDTO<FollowerResponse> getFollowers(Long userId, int page, int pageSize);

    PageDTO<FollowingResponse> getFollowing(Long userId, int page, int pageSize);

    boolean isFollowing(Long currentUserId, Long targetUserId);
}
