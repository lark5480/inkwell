package com.blog.repository;

import com.blog.entity.ArticleViewHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleViewHistoryRepository extends JpaRepository<ArticleViewHistory, Long> {

    Page<ArticleViewHistory> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    @Query("SELECT h FROM ArticleViewHistory h WHERE h.userId = :userId " +
           "AND (:keyword IS NULL OR h.articleTitle LIKE %:keyword%) " +
           "ORDER BY h.createTime DESC")
    Page<ArticleViewHistory> findByUserIdAndKeyword(@Param("userId") Long userId,
                                                     @Param("keyword") String keyword,
                                                     Pageable pageable);

    void deleteByIdAndUserId(Long id, Long userId);
}
