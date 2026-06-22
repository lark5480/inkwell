package com.blog.repository;

import com.blog.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 与某用户的消息历史
    Page<Message> findByFromUserIdAndToUserIdOrFromUserIdAndToUserIdOrderByCreateTimeDesc(
            Long fromUserId1, Long toUserId1, Long fromUserId2, Long toUserId2, Pageable pageable);

    // 未读消息数
    long countByToUserIdAndIsRead(Long toUserId, Boolean isRead);

    // 与某人的未读数
    long countByToUserIdAndFromUserIdAndIsRead(Long toUserId, Long fromUserId, Boolean isRead);

    // 标记与某人的所有消息已读
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.toUserId = :toUserId AND m.fromUserId = :fromUserId AND m.isRead = false")
    void markAllRead(@Param("toUserId") Long toUserId, @Param("fromUserId") Long fromUserId);

    // 获取某个用户的所有会话对方ID（去重）
    @Query("SELECT DISTINCT m.fromUserId FROM Message m WHERE m.toUserId = :userId " +
           "UNION SELECT DISTINCT m.toUserId FROM Message m WHERE m.fromUserId = :userId")
    List<Long> findConversationUserIds(@Param("userId") Long userId);

    // 获取两人之间的最后一条消息
    @Query("SELECT m FROM Message m WHERE " +
           "(m.fromUserId = :user1 AND m.toUserId = :user2) OR (m.fromUserId = :user2 AND m.toUserId = :user1) " +
           "ORDER BY m.createTime DESC")
    List<Message> findLastMessageBetween(@Param("user1") Long user1, @Param("user2") Long user2, Pageable pageable);
}
