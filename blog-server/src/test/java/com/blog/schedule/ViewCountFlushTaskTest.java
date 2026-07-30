package com.blog.schedule;

import com.blog.common.CacheNames;
import com.blog.service.CountFlushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ViewCountFlushTask")
class ViewCountFlushTaskTest {

    private static final String VIEW_PROCESSING = CacheNames.VIEW_COUNT_HASH + ":processing";
    private static final String LIKE_PROCESSING = CacheNames.LIKE_COUNT_HASH + ":processing";

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private CountFlushService countFlushService;

    private ViewCountFlushTask task;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        task = new ViewCountFlushTask(redisTemplate, countFlushService);
    }

    @Nested
    @DisplayName("flushHash - RENAME 原子快照")
    class FlushHashRename {

        @Test
        @DisplayName("should skip when renameIfAbsent returns false (previous task not done)")
        void shouldSkip_whenRenameFails() {
            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(false);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(false);

            task.flushViewAndLikeCount();

            verify(countFlushService, never()).flushCounts(any(), anyBoolean(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("should skip when renameIfAbsent returns null (Redis connection error)")
        void shouldSkip_whenRenameReturnsNull() {
            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(null);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(null);

            task.flushViewAndLikeCount();

            // null 应被视为失败，不执行回写
            verify(countFlushService, never()).flushCounts(any(), anyBoolean(), anyString(), anyBoolean());
            // 不应设置 TTL
            verify(redisTemplate, never()).expire(anyString(), eq(10L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("should set TTL on processingKey after successful rename")
        void shouldSetTtl_afterSuccessfulRename() {
            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(VIEW_PROCESSING)).willReturn(true);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(LIKE_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(LIKE_PROCESSING)).willReturn(true);

            task.flushViewAndLikeCount();

            verify(redisTemplate).expire(eq(VIEW_PROCESSING), eq(10L), eq(TimeUnit.MINUTES));
            verify(redisTemplate).expire(eq(LIKE_PROCESSING), eq(10L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("should skip when processingKey hash is empty after rename")
        void shouldSkip_whenProcessingKeyIsEmpty() {
            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(VIEW_PROCESSING)).willReturn(true);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(LIKE_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(LIKE_PROCESSING)).willReturn(true);

            task.flushViewAndLikeCount();

            verify(redisTemplate).delete(VIEW_PROCESSING);
            verify(countFlushService, never()).flushCounts(any(), anyBoolean(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("should call flushCounts with correct view count batch")
        void shouldCallFlushCounts_forViewCounts() {
            Map<Object, Object> entries = new LinkedHashMap<>();
            entries.put("1", 10);
            entries.put("2", 5);

            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(entries);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(LIKE_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(LIKE_PROCESSING)).willReturn(true);

            task.flushViewAndLikeCount();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<long[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
            verify(countFlushService).flushCounts(
                    batchCaptor.capture(),
                    eq(true),
                    eq(VIEW_PROCESSING),
                    eq(true)
            );

            List<long[]> batch = batchCaptor.getValue();
            assertThat(batch).hasSize(2);
            assertThat(batch.get(0)).containsExactly(1L, 10L);
            assertThat(batch.get(1)).containsExactly(2L, 5L);
        }

        @Test
        @DisplayName("should call flushCounts with correct like count batch")
        void shouldCallFlushCounts_forLikeCounts() {
            Map<Object, Object> entries = new LinkedHashMap<>();
            entries.put("100", 3);

            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(VIEW_PROCESSING)).willReturn(true);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(LIKE_PROCESSING)).willReturn(entries);

            task.flushViewAndLikeCount();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<long[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
            verify(countFlushService).flushCounts(
                    batchCaptor.capture(),
                    eq(false),
                    eq(LIKE_PROCESSING),
                    eq(true)
            );

            List<long[]> batch = batchCaptor.getValue();
            assertThat(batch).hasSize(1);
            assertThat(batch.get(0)).containsExactly(100L, 3L);
        }

        @Test
        @DisplayName("should skip entries with null, zero or negative delta")
        void shouldSkipNullOrZeroDelta() {
            Map<Object, Object> entries = new LinkedHashMap<>();
            entries.put("1", 0);    // zero → skip
            entries.put("2", -1);   // negative → skip
            entries.put("abc", 5);  // unparseable id → skip

            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(entries);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(LIKE_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(LIKE_PROCESSING)).willReturn(true);

            task.flushViewAndLikeCount();

            // All entries invalid → processingKey deleted, no DB call
            verify(redisTemplate).delete(VIEW_PROCESSING);
            verify(countFlushService, never()).flushCounts(any(), anyBoolean(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("should handle String keys and values")
        void shouldHandleStringKeysAndValues() {
            Map<Object, Object> entries = new LinkedHashMap<>();
            entries.put("123", "25");

            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(LIKE_PROCESSING)).willReturn(entries);
            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(VIEW_PROCESSING)).willReturn(true);

            task.flushViewAndLikeCount();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<long[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
            verify(countFlushService).flushCounts(
                    batchCaptor.capture(),
                    eq(false),
                    eq(LIKE_PROCESSING),
                    eq(true)
            );

            List<long[]> batch = batchCaptor.getValue();
            assertThat(batch).hasSize(1);
            assertThat(batch.get(0)).containsExactly(123L, 25L);
        }
    }

    @Nested
    @DisplayName("onShutdown - 优雅关闭")
    class OnShutdown {

        @Test
        @DisplayName("should merge residual processingKey before final flush")
        void shouldMergeResidualProcessingKeys() {
            Map<Object, Object> residual = new LinkedHashMap<>();
            residual.put("42", 7);

            // mergeProcessingKeys: VIEW processing key has residual data
            given(redisTemplate.hasKey(VIEW_PROCESSING)).willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(residual);
            // After merge, the main key will contain {42: 7}, then flushHash is called
            // renameIfAbsent will succeed (processing key was deleted by mergeProcessingKeys)
            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(residual);

            // LIKE processing key has no residual
            given(redisTemplate.hasKey(LIKE_PROCESSING)).willReturn(false);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(LIKE_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(LIKE_PROCESSING)).willReturn(true);

            task.onShutdown();

            // mergeProcessingKeys puts residual data back into main key
            verify(hashOperations).putAll(eq(CacheNames.VIEW_COUNT_HASH), eq(residual));
            verify(redisTemplate).delete(VIEW_PROCESSING);

            // Then flushHash processes the merged data
            verify(countFlushService).flushCounts(any(), eq(true), eq(VIEW_PROCESSING), eq(true));
        }

        @Test
        @DisplayName("should do nothing when no residual processingKeys and no new data")
        void shouldDoNothing_whenNoResidualAndNoData() {
            given(redisTemplate.hasKey(VIEW_PROCESSING)).willReturn(false);
            given(redisTemplate.hasKey(LIKE_PROCESSING)).willReturn(false);
            given(redisTemplate.renameIfAbsent(CacheNames.VIEW_COUNT_HASH, VIEW_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(VIEW_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(VIEW_PROCESSING)).willReturn(true);
            given(redisTemplate.renameIfAbsent(CacheNames.LIKE_COUNT_HASH, LIKE_PROCESSING))
                    .willReturn(true);
            given(hashOperations.entries(LIKE_PROCESSING)).willReturn(Collections.emptyMap());
            given(redisTemplate.delete(LIKE_PROCESSING)).willReturn(true);

            task.onShutdown();

            verify(countFlushService, never()).flushCounts(any(), anyBoolean(), anyString(), anyBoolean());
        }
    }
}
