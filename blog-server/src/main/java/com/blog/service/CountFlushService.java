package com.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 批量回写 Redis 计数到 MySQL 的事务服务。
 * <p>
 * 独立为 Service 以确保 Spring AOP 代理能正确拦截 @Transactional，
 * 避免同类内部调用导致事务失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CountFlushService {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 将计数增量批量写入 MySQL，事务提交后自动删除 Redis processingKey。
     * <p>
     * afterCommit 回调在整个方法事务提交后执行，保证 MySQL 和 Redis 的一致性：
     * 只有 MySQL 事务成功提交，才会删除 processingKey。
     *
     * @param batch         每行 {articleId, delta}
     * @param isViewCount   true=浏览量，false=点赞量
     * @param processingKey Redis 临时快照 key，事务提交后删除
     * @param isLastBatch   是否为最后一批，只有最后一批才注册 afterCommit 回调删除 processingKey
     */
    @Transactional
    public void flushCounts(List<long[]> batch, boolean isViewCount, String processingKey, boolean isLastBatch) {
        String sql = isViewCount
                ? "UPDATE articles SET view_count = view_count + ? WHERE id = ?"
                : "UPDATE articles SET like_count = like_count + ? WHERE id = ?";

        jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, args) -> {
            ps.setLong(1, args[1]); // delta
            ps.setLong(2, args[0]); // articleId
        });

        // 只有最后一批才注册 afterCommit 回调：MySQL 事务提交成功后删除 processingKey
        if (isLastBatch) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            redisTemplate.delete(processingKey);
                            log.info("MySQL 事务提交成功，已清理 Redis processingKey: {}", processingKey);
                        }
                    }
            );
        }
    }
}
