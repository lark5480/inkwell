package com.blog.repository;

import com.blog.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type, Pageable pageable);

    long countByUserIdAndIsRead(Long userId, Boolean isRead);

    long countByUserIdAndTypeAndIsRead(Long userId, String type, Boolean isRead);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.fromUserId = :fromUserId AND n.type = 'MESSAGE' AND n.isRead = false")
    void markMessageNotificationsAsRead(@Param("userId") Long userId, @Param("fromUserId") Long fromUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.type = :type AND n.isRead = false")
    void markTypeNotificationsAsRead(@Param("userId") Long userId, @Param("type") String type);
}
