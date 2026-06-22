package com.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 站内通知记录
 * type: COMMENT(评论) / LIKE(点赞) / FOLLOW(关注) / MESSAGE(私信)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_user_time", columnList = "user_id, create_time DESC")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "from_user_name", length = 50)
    private String fromUserName;

    @Column(name = "from_user_avatar", length = 255)
    private String fromUserAvatar;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "article_title", length = 200)
    private String articleTitle;

    @Column(name = "article_slug", length = 200)
    private String articleSlug;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false, columnDefinition = "TINYINT")
    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
