package com.blog.repository;

import com.blog.entity.ArticleLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    Page<ArticleLike> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    void deleteByUserIdAndArticleId(Long userId, Long articleId);
}
