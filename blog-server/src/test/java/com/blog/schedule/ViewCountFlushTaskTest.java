package com.blog.schedule;

import com.blog.common.CacheNames;
import com.blog.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ViewCountFlushTask")
class ViewCountFlushTaskTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ArticleRepository articleRepository;

    private ViewCountFlushTask task;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        task = new ViewCountFlushTask(redisTemplate, articleRepository);
    }

    @Nested
    @DisplayName("flushHash")
    class FlushHash {

        @Test
        @DisplayName("should skip when Redis Hash is empty")
        void shouldSkip_whenHashIsEmpty() {
            given(hashOperations.entries(CacheNames.VIEW_COUNT_HASH)).willReturn(Collections.emptyMap());

            task.flushViewAndLikeCount();

            verify(articleRepository, never()).addViewCount(anyLong(), anyLong());
        }

        @Test
        @DisplayName("should update view counts and delete only processed entries")
        void shouldUpdateViewCounts_andDeleteProcessedEntries() {
            Map<Object, Object> entries = new LinkedHashMap<>();
            entries.put("1", 10);
            entries.put("2", 5);

            given(hashOperations.entries(CacheNames.VIEW_COUNT_HASH)).willReturn(entries);
            given(hashOperations.entries(CacheNames.LIKE_COUNT_HASH)).willReturn(Collections.emptyMap());

            task.flushViewAndLikeCount();

            verify(articleRepository).addViewCount(1L, 10L);
            verify(articleRepository).addViewCount(2L, 5L);
            // 只删除处理成功的条目，而非整个 Hash
            verify(hashOperations).delete(CacheNames.VIEW_COUNT_HASH, "1");
            verify(hashOperations).delete(CacheNames.VIEW_COUNT_HASH, "2");
        }

        @Test
        @DisplayName("should retain failed entries in Redis for next retry")
        void shouldRetainFailedEntries_forRetry() {
            Map<Object, Object> entries = new LinkedHashMap<>();
            entries.put("1", 10);
            entries.put("2", 5);  // This one will fail

            given(hashOperations.entries(CacheNames.VIEW_COUNT_HASH)).willReturn(entries);
            given(hashOperations.entries(CacheNames.LIKE_COUNT_HASH)).willReturn(Collections.emptyMap());
            // Article 2 causes an exception
            doThrow(new RuntimeException("DB error")).when(articleRepository).addViewCount(2L, 5L);

            task.flushViewAndLikeCount();

            // Article 1 succeeds and is deleted from Redis
            verify(articleRepository).addViewCount(1L, 10L);
            verify(hashOperations).delete(CacheNames.VIEW_COUNT_HASH, "1");

            // Article 2 fails — NOT deleted from Redis (retained for retry)
            verify(hashOperations, never()).delete(CacheNames.VIEW_COUNT_HASH, "2");
        }

        @Test
        @DisplayName("should handle String keys and values")
        void shouldHandleStringKeysAndValues() {
            Map<Object, Object> entries = new LinkedHashMap<>();
            entries.put("123", "25");

            given(hashOperations.entries(CacheNames.LIKE_COUNT_HASH)).willReturn(entries);
            given(hashOperations.entries(CacheNames.VIEW_COUNT_HASH)).willReturn(Collections.emptyMap());

            task.flushViewAndLikeCount();

            verify(articleRepository).addLikeCount(123L, 25);
            verify(hashOperations).delete(CacheNames.LIKE_COUNT_HASH, "123");
        }

        @Test
        @DisplayName("should skip entries with null or zero delta")
        void shouldSkipNullOrZeroDelta() {
            Map<Object, Object> entries = new LinkedHashMap<>();
            entries.put("1", 0);   // zero — skip
            entries.put("2", -1);  // negative — skip

            given(hashOperations.entries(CacheNames.VIEW_COUNT_HASH)).willReturn(entries);
            given(hashOperations.entries(CacheNames.LIKE_COUNT_HASH)).willReturn(Collections.emptyMap());

            task.flushViewAndLikeCount();

            verify(articleRepository, never()).addViewCount(anyLong(), anyLong());
        }
    }
}
