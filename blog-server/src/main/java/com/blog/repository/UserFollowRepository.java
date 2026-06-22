package com.blog.repository;

import com.blog.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Modifying
    @Query("DELETE FROM UserFollow f WHERE f.followerId = :followerId AND f.followingId = :followingId")
    void deleteByFollowerIdAndFollowingId(@Param("followerId") Long followerId,
                                          @Param("followingId") Long followingId);

    long countByFollowingId(Long followingId);

    long countByFollowerId(Long followerId);

    /** 获取某用户的粉丝列表（关注该用户的人） */
    Page<UserFollow> findByFollowingIdOrderByCreateTimeDesc(Long followingId, Pageable pageable);

    /** 获取某用户的关注列表（该用户关注的人） */
    Page<UserFollow> findByFollowerIdOrderByCreateTimeDesc(Long followerId, Pageable pageable);
}
