package com.blog.schedule;

import com.blog.common.CacheNames;
import com.blog.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 定时把 Redis 中的浏览量/点赞量增量回写到 MySQL。
 * 每 5 分钟执行一次，回写后清空对应 Hash。
 */
@Component
public class ViewCountFlushTask {

    private static final Logger log = LoggerFactory.getLogger(ViewCountFlushTask.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ArticleRepository articleRepository;

    public ViewCountFlushTask(RedisTemplate<String, Object> redisTemplate,
                              ArticleRepository articleRepository) {
        this.redisTemplate = redisTemplate;
        this.articleRepository = articleRepository;
    }

    @Scheduled(fixedDelayString = "${blog.cache.view-count-flush-interval:300000}")
    @Transactional
    public void flushViewAndLikeCount() {
        flushHash(CacheNames.VIEW_COUNT_HASH, true);
        flushHash(CacheNames.LIKE_COUNT_HASH, false);
    }

    private void flushHash(String hashKey, boolean isViewCount) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(hashKey);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        int success = 0;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                Long articleId = parseId(entry.getKey());
                Number delta = parseNumber(entry.getValue());
                if (articleId == null || delta == null || delta.longValue() <= 0) {
                    continue;
                }
                if (isViewCount) {
                    articleRepository.addViewCount(articleId, delta.longValue());
                } else {
                    articleRepository.addLikeCount(articleId, delta.intValue());
                }
                /* 仅删除成功回写的条目，失败条目保留供下次重试 */
                redisTemplate.opsForHash().delete(hashKey, entry.getKey());
                success++;
            } catch (Exception e) {
                log.error("回写 {} 失败 entry={}", hashKey, entry, e);
            }
        }

        log.info("{} 回写完成，成功 {} 条，剩余 {} 条待下次重试",
                hashKey, success, entries.size() - success);
    }

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
