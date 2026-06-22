package com.blog.repository;

import com.blog.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query(value = "SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.user LEFT JOIN FETCH a.category LEFT JOIN FETCH a.tags WHERE a.status = :status AND a.isDeleted = :isDeleted",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.status = :status AND a.isDeleted = :isDeleted")
    Page<Article> findByStatusAndIsDeleted(@Param("status") String status, @Param("isDeleted") Boolean isDeleted, Pageable pageable);

    @Query(value = "SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.user LEFT JOIN FETCH a.category LEFT JOIN FETCH a.tags WHERE a.category.id = :categoryId AND a.status = :status AND a.isDeleted = :isDeleted",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.category.id = :categoryId AND a.status = :status AND a.isDeleted = :isDeleted")
    Page<Article> findByCategoryIdAndStatusAndIsDeleted(@Param("categoryId") Long categoryId, @Param("status") String status, @Param("isDeleted") Boolean isDeleted, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "category", "tags"})
    Optional<Article> findBySlugAndIsDeleted(String slug, Boolean isDeleted);

    long countByStatus(String status);

    boolean existsBySlug(String slug);

    @Modifying
    @Query("UPDATE Article a SET a.commentCount = :count WHERE a.id = :id")
    void updateCommentCount(@Param("id") Long id, @Param("count") int count);

    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void updateViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + :delta WHERE a.id = :id")
    void addViewCount(@Param("id") Long id, @Param("delta") long delta);

    @Modifying
    @Query("UPDATE Article a SET a.likeCount = a.likeCount + 1 WHERE a.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.likeCount = a.likeCount + :delta WHERE a.id = :id")
    void addLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Query(value = "SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.user LEFT JOIN FETCH a.category LEFT JOIN FETCH a.tags WHERE a.user.id = :userId",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.user.id = :userId")
    Page<Article> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.user LEFT JOIN FETCH a.category LEFT JOIN FETCH a.tags WHERE a.user.id = :userId AND a.status = :status",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.user.id = :userId AND a.status = :status")
    Page<Article> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.user LEFT JOIN FETCH a.category LEFT JOIN FETCH a.tags WHERE a.user.id = :userId AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword%)",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.user.id = :userId AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword%)")
    Page<Article> findByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.user LEFT JOIN FETCH a.category LEFT JOIN FETCH a.tags WHERE a.user.id = :userId AND a.status = :status AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword%)",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.user.id = :userId AND a.status = :status AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword%)")
    Page<Article> findByUserIdAndStatusAndKeyword(@Param("userId") Long userId, @Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    long countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT COALESCE(SUM(a.viewCount), 0) FROM Article a WHERE a.isDeleted = false AND a.status = 'PUBLISHED'")
    long sumViewCount();
}
