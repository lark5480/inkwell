package com.blog.schedule;

import com.blog.common.CacheNames;
import com.blog.service.CountFlushService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 定时把 Redis 中的浏览量/点赞量增量回写到 MySQL。
 * <p>
 * 采用 RENAME 原子快照方案：先将 Hash 原子重命名为 processing key，
 * 再回写 MySQL，事务提交后才删除 processing key，消除竞态窗口。
 * 每 5 分钟执行一次。
 */
@Component
public class ViewCountFlushTask {

    private static final Logger log = LoggerFactory.getLogger(ViewCountFlushTask.class);

    private static final String PROCESSING_SUFFIX = ":processing";
    private static final long PROCESSING_KEY_TTL_MINUTES = 10;

    private final RedisTemplate<String, Object> redisTemplate;
    private final CountFlushService countFlushService;

    public ViewCountFlushTask(RedisTemplate<String, Object> redisTemplate,
                              CountFlushService countFlushService) {
        this.redisTemplate = redisTemplate;
        this.countFlushService = countFlushService;
    }

    @Scheduled(fixedDelayString = "${blog.cache.view-count-flush-interval:300000}")
    public void flushViewAndLikeCount() {
        flushHash(CacheNames.VIEW_COUNT_HASH, true);
        flushHash(CacheNames.LIKE_COUNT_HASH, false);
    }

    private void flushHash(String hashKey, boolean isViewCount) {
        String processingKey = hashKey + PROCESSING_SUFFIX;

        // 0. 源 key 不存在则无需回写（Redis RENAME 对不存在的 key 会报 ERR no such key）
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(hashKey))) {
            return;
        }

        // 1. 原子 RENAME NX：将当前 Hash 快照转移到 processing key
        //    若 processingKey 已存在（上次回写未完成），则跳过本次
        Boolean renamed = redisTemplate.renameIfAbsent(hashKey, processingKey);
        if (!Boolean.TRUE.equals(renamed)) {
            log.warn("回写任务 {} 上次未完成，跳过本次", hashKey);
            return;
        }

        // 2. 为 processingKey 设置 TTL 兜底，防止异常情况下永久存在
        redisTemplate.expire(processingKey, PROCESSING_KEY_TTL_MINUTES, TimeUnit.MINUTES);

        // 3. 读取 processingKey 中的所有数据
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(processingKey);
        if (entries.isEmpty()) {
            redisTemplate.delete(processingKey);
            return;
        }

        // 4. 过滤有效条目
        List<long[]> batch = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long articleId = parseId(entry.getKey());
            Number delta = parseNumber(entry.getValue());
            if (articleId == null || delta == null || delta.longValue() <= 0) {
                continue;
            }
            batch.add(new long[]{articleId, delta.longValue()});
        }

        if (batch.isEmpty()) {
            redisTemplate.delete(processingKey);
            return;
        }

        // 5. 分批回写 MySQL（每批最多 500 条），事务提交后自动删 Redis processingKey
        int batchSize = 500;
        try {
            for (int i = 0; i < batch.size(); i += batchSize) {
                List<long[]> subBatch = batch.subList(i, Math.min(i + batchSize, batch.size()));
                boolean isLastBatch = (i + batchSize >= batch.size());
                countFlushService.flushCounts(subBatch, isViewCount, processingKey, isLastBatch);
            }
        } catch (Exception e) {
            log.error("回写 {} 失败，保留 processingKey 供下次重试", hashKey, e);
            return;
        }

        log.info("{} 回写完成，共 {} 条有效数据", hashKey, batch.size());
    }

    // -------------------------------------------------------------------------
    // 优雅关闭
    // -------------------------------------------------------------------------

    @PreDestroy
    public void onShutdown() {
        log.info("应用关闭，执行最终数据回写...");
        mergeProcessingKeys();
        flushViewAndLikeCount();
        log.info("关闭前回写完成");
    }

    /**
     * 将残留的 processingKey 数据合并回主 key，以便 flushViewAndLikeCount 统一处理。
     */
    private void mergeProcessingKeys() {
        String[] processingKeys = {
                CacheNames.VIEW_COUNT_HASH + PROCESSING_SUFFIX,
                CacheNames.LIKE_COUNT_HASH + PROCESSING_SUFFIX
        };
        for (String pk : processingKeys) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(pk))) {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(pk);
                if (!entries.isEmpty()) {
                    String mainKey = pk.replace(PROCESSING_SUFFIX, "");
                    redisTemplate.opsForHash().putAll(mainKey, entries);
                }
                redisTemplate.delete(pk);
                log.info("已合并残留 processingKey: {} 到 {}", pk, pk.replace(PROCESSING_SUFFIX, ""));
            }
        }
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    private Long parseId(Object key) {
        if (key == null) return null;
        if (key instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(key.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Number parseNumber(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
