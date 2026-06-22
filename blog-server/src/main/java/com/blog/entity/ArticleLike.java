package com.blog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 用户点赞记录（每个用户对每篇文章只能点赞一次）
 * 无软删除
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "article_likes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_article", columnNames = {"user_id", "article_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class ArticleLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "article_title", length = 200)
    private String articleTitle;

    @Column(name = "article_slug", length = 200)
    private String articleSlug;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
