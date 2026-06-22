package com.blog.service;

import com.blog.common.CacheNames;
import com.blog.dto.*;
import com.blog.entity.*;
import com.blog.exception.BusinessException;
import com.blog.repository.*;
import com.blog.service.impl.ArticleServiceImpl;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;
    @Mock private ArticleViewHistoryRepository viewHistoryRepository;
    @Mock private ArticleLikeRepository articleLikeRepository;
    @Mock private EntityManager entityManager;
    @Mock private MarkdownRenderer markdownRenderer;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Captor
    private ArgumentCaptor<Article> articleCaptor;

    private User testUser;
    private Category testCategory;
    private Tag testTag;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("admin")
                .nickname("Admin")
                .role("ADMIN")
                .build();

        testCategory = Category.builder()
                .id(1L)
                .name("Tech")
                .slug("tech")
                .build();

        testTag = Tag.builder()
                .id(1L)
                .name("Java")
                .slug("java")
                .build();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== createArticle ==========

    @Test
    void createArticle_shouldGenerateSlug_whenNotProvided() {
        // Arrange
        var request = new ArticleCreateRequest(
                "Test Title", null, "Content", "Summary",
                null, null, null, "DRAFT", null
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(articleRepository.save(any(Article.class))).willAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        // Act
        Long id = articleService.createArticle(request, 1L);

        // Assert
        assertThat(id).isEqualTo(100L);
        verify(articleRepository).save(articleCaptor.capture());
        Article saved = articleCaptor.getValue();
        assertThat(saved.getSlug()).isNotNull().isNotEmpty();
        // slug should be auto-generated (8 chars from UUID)
        assertThat(saved.getSlug()).hasSize(8);
        assertThat(saved.getTitle()).isEqualTo("Test Title");
        assertThat(saved.getStatus()).isEqualTo("DRAFT");
        assertThat(saved.getUser()).isEqualTo(testUser);
    }

    @Test
    void createArticle_shouldHandleDuplicateSlug() {
        // Arrange
        var request = new ArticleCreateRequest(
                "Test Title", "my-slug", "Content", "Summary",
                null, null, null, "DRAFT", null
        );
        given(articleRepository.existsBySlug("my-slug")).willReturn(true, false);
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(articleRepository.save(any(Article.class))).willAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(101L);
            return a;
        });

        // Act
        Long id = articleService.createArticle(request, 1L);

        // Assert
        assertThat(id).isEqualTo(101L);
        verify(articleRepository).save(articleCaptor.capture());
        Article saved = articleCaptor.getValue();
        // Should have the random suffix appended
        assertThat(saved.getSlug()).startsWith("my-slug-");
        assertThat(saved.getSlug()).hasSize(12); // "my-slug-" (8) + 4 random chars
    }

    @Test
    void createArticle_shouldSetPublishedAt_whenStatusIsPublished() {
        // Arrange
        var request = new ArticleCreateRequest(
                "Published Article", "published-slug", "Content", "Summary",
                null, 1L, Set.of(1L), "PUBLISHED", null
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory));
        given(tagRepository.findAllById(Set.of(1L))).willReturn(List.of(testTag));
        given(articleRepository.existsBySlug("published-slug")).willReturn(false);
        given(articleRepository.save(any(Article.class))).willAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(102L);
            return a;
        });

        // Act
        articleService.createArticle(request, 1L);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        Article saved = articleCaptor.getValue();
        assertThat(saved.getPublishedAt()).isNotNull();
        assertThat(saved.getCategory()).isEqualTo(testCategory);
        assertThat(saved.getTags()).containsExactly(testTag);
    }

    // ========== getArticleBySlug ==========

    @Test
    void getArticleBySlug_shouldThrow404_whenNotFound() {
        // Arrange
        given(articleRepository.findBySlugAndIsDeleted("non-existent", false))
                .willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> articleService.getArticleBySlug("non-existent"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void getArticleBySlug_shouldIncrementViewCount() {
        // Arrange
        Article article = Article.builder()
                .id(1L).title("Test").slug("test-article")
                .content("Hello World").status("PUBLISHED")
                .viewCount(10L).likeCount(0).commentCount(0).isTop(false)
                .user(testUser).build();

        given(articleRepository.findBySlugAndIsDeleted("test-article", false))
                .willReturn(Optional.of(article));
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.increment(CacheNames.VIEW_COUNT_HASH, "1", 1))
                .willReturn(3L);
        given(markdownRenderer.renderForArticle(1L, "Hello World"))
                .willReturn("<p>Hello World</p>");
        // Anonymous user - no auth context set
        var auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        ArticleDetailResponse response = articleService.getArticleBySlug("test-article");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo("test-article");
        // DB viewCount (10) + Redis increment (3) = 13
        assertThat(response.viewCount()).isEqualTo(13L);
        // Anonymous user: no view history recorded
        verify(viewHistoryRepository, never()).save(any());
    }

    @Test
    void getArticleBySlug_shouldRecordViewHistory_forAuthenticatedUser() {
        // Arrange
        Article article = Article.builder()
                .id(1L).title("Test").slug("test-article")
                .content("Hello World").status("PUBLISHED")
                .viewCount(10L).likeCount(0).commentCount(0).isTop(false)
                .user(testUser).build();

        given(articleRepository.findBySlugAndIsDeleted("test-article", false))
                .willReturn(Optional.of(article));
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.increment(CacheNames.VIEW_COUNT_HASH, "1", 1))
                .willReturn(1L);
        given(markdownRenderer.renderForArticle(1L, "Hello World"))
                .willReturn("<p>Hello World</p>");

        // Authenticated user
        var auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        ArticleDetailResponse response = articleService.getArticleBySlug("test-article");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo("test-article");
        assertThat(response.isLiked()).isFalse();
        verify(viewHistoryRepository).save(any(ArticleViewHistory.class));
    }

    // ========== likeArticle ==========

    @Test
    void likeArticle_authenticated_shouldAddLike() {
        // Arrange
        Article article = Article.builder()
                .id(1L).title("Test").slug("test-article")
                .content("Content").status("PUBLISHED")
                .viewCount(5L).likeCount(2).commentCount(0).isTop(false)
                .user(testUser).build();

        given(articleRepository.findBySlugAndIsDeleted("test-article", false))
                .willReturn(Optional.of(article));
        given(articleLikeRepository.existsByUserIdAndArticleId(1L, 1L))
                .willReturn(false);

        var auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        LikeToggleResponse response = articleService.likeArticle("test-article");

        // Assert
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(3); // 2 + 1
        verify(articleLikeRepository).save(any(ArticleLike.class));
        verify(articleRepository).save(article);
    }

    @Test
    void likeArticle_authenticated_shouldRemoveLike() {
        // Arrange
        Article article = Article.builder()
                .id(1L).title("Test").slug("test-article")
                .content("Content").status("PUBLISHED")
                .viewCount(5L).likeCount(3).commentCount(0).isTop(false)
                .user(testUser).build();

        given(articleRepository.findBySlugAndIsDeleted("test-article", false))
                .willReturn(Optional.of(article));
        given(articleLikeRepository.existsByUserIdAndArticleId(1L, 1L))
                .willReturn(true);

        var auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        LikeToggleResponse response = articleService.likeArticle("test-article");

        // Assert
        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(2); // 3 - 1
        verify(articleLikeRepository).deleteByUserIdAndArticleId(1L, 1L);
        verify(articleRepository).save(article);
    }

    @Test
    void likeArticle_anonymous_shouldIncrementRedisCounter() {
        // Arrange
        Article article = Article.builder()
                .id(1L).title("Test").slug("test-article")
                .content("Content").status("PUBLISHED")
                .viewCount(5L).likeCount(2).commentCount(0).isTop(false)
                .user(testUser).build();

        given(articleRepository.findBySlugAndIsDeleted("test-article", false))
                .willReturn(Optional.of(article));
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.increment(CacheNames.LIKE_COUNT_HASH, "1", 1))
                .willReturn(1L);

        // Anonymous user
        var auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        LikeToggleResponse response = articleService.likeArticle("test-article");

        // Assert
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(3); // 2 + 1
        verify(articleLikeRepository, never()).save(any());
        verify(articleLikeRepository, never()).deleteByUserIdAndArticleId(anyLong(), anyLong());
    }

    // ========== updateArticle ==========

    @Test
    void updateArticle_shouldUpdateFieldsAndEvictCaches() {
        // Arrange
        Article existing = Article.builder()
                .id(1L).title("Old Title").slug("old-slug")
                .content("Old Content").status("DRAFT")
                .viewCount(0L).likeCount(0).commentCount(0).isTop(false)
                .user(testUser).build();

        var request = new ArticleUpdateRequest(
                "New Title", "new-slug", "New Content",
                "New Summary", "https://example.com/cover.jpg",
                1L, Set.of(1L), "PUBLISHED", null
        );

        given(articleRepository.findById(1L)).willReturn(Optional.of(existing));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory));
        given(tagRepository.findAllById(Set.of(1L))).willReturn(List.of(testTag));

        // Act
        articleService.updateArticle(1L, request);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        Article saved = articleCaptor.getValue();
        assertThat(saved.getTitle()).isEqualTo("New Title");
        assertThat(saved.getSlug()).isEqualTo("new-slug");
        assertThat(saved.getContent()).isEqualTo("New Content");
        assertThat(saved.getSummary()).isEqualTo("New Summary");
        assertThat(saved.getCoverImage()).isEqualTo("https://example.com/cover.jpg");
        assertThat(saved.getCategory()).isEqualTo(testCategory);
        assertThat(saved.getTags()).containsExactly(testTag);
        // Draft -> Published: should set publishedAt
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    // ========== deleteArticle (soft delete) ==========

    @Test
    void deleteArticle_shouldSetIsDeleted_whenCalled() {
        // Arrange
        Article article = Article.builder()
                .id(1L).title("To Delete").slug("to-delete")
                .content("Content").status("DRAFT")
                .viewCount(0L).likeCount(0).commentCount(0).isTop(false)
                .user(testUser).build();

        given(articleRepository.findById(1L)).willReturn(Optional.of(article));
        given(articleRepository.save(any(Article.class))).willReturn(article);

        // Act
        articleService.deleteArticle(1L);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        Article saved = articleCaptor.getValue();
        assertThat(saved.getIsDeleted()).isTrue();
    }

    // ========== getAdminArticlePage ==========

    @Test
    void getAdminArticlePage_shouldReturnPagedResults() {
        // Arrange
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Article> cq = mock(CriteriaQuery.class);
        Root<Article> root = mock(Root.class);
        Fetch<?, ?> fetch = mock(Fetch.class);
        CriteriaQuery<Long> countCq = mock(CriteriaQuery.class);
        Root<Article> countRoot = mock(Root.class);
        TypedQuery<Article> typedQuery = mock(TypedQuery.class);
        TypedQuery<Long> countTypedQuery = mock(TypedQuery.class);

        given(entityManager.getCriteriaBuilder()).willReturn(cb);
        given(cb.createQuery(Article.class)).willReturn(cq);
        given(cq.from(Article.class)).willReturn(root);
        doReturn(fetch).when(root).fetch("category", JoinType.LEFT);
        doReturn(fetch).when(root).fetch("tags", JoinType.LEFT);
        doReturn(fetch).when(root).fetch("user", JoinType.LEFT);
        given(cb.equal(root.get("isDeleted"), false)).willReturn(mock(Predicate.class));
        given(cq.where(any(Predicate[].class))).willReturn(cq);
        given(cq.distinct(true)).willReturn(cq);
        doReturn(cq).when(cq).orderBy((Order) null);

        // Count query
        given(cb.createQuery(Long.class)).willReturn(countCq);
        given(countCq.from(Article.class)).willReturn(countRoot);
        given(cb.equal(countRoot.get("isDeleted"), false)).willReturn(mock(Predicate.class));
        given(countCq.select(any())).willReturn(countCq);
        given(countCq.where(any(Predicate[].class))).willReturn(countCq);

        given(entityManager.createQuery(countCq)).willReturn(countTypedQuery);
        given(countTypedQuery.getSingleResult()).willReturn(1L);

        // Build a real article for the result list
        Article article = Article.builder()
                .id(1L).title("Test").slug("test")
                .content("Content").status("PUBLISHED")
                .viewCount(5L).likeCount(2).commentCount(0).isTop(false)
                .user(testUser).category(testCategory).build();

        given(entityManager.createQuery(cq)).willReturn(typedQuery);
        given(typedQuery.setFirstResult(anyInt())).willReturn(typedQuery);
        given(typedQuery.setMaxResults(anyInt())).willReturn(typedQuery);
        given(typedQuery.getResultList()).willReturn(List.of(article));

        // Act
        PageDTO<ArticleAdminResponse> result = articleService.getAdminArticlePage(1, 10, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.records().get(0).title()).isEqualTo("Test");
    }
}
