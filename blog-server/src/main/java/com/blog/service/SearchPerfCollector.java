package com.blog.service;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 搜索性能统计收集器
 * 记录搜索请求的耗时、缓存命中率等指标，内存中保留最近 1000 条记录。
 */
@Slf4j
@Service
public class SearchPerfCollector {

    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final ConcurrentLinkedDeque<Long> responseTimes = new ConcurrentLinkedDeque<>();

    /**
     * 记录一次搜索
     *
     * @param durationNanos 搜索耗时（纳秒）
     * @param cacheHit      是否命中缓存
     */
    public void recordSearch(long durationNanos, boolean cacheHit) {
        totalSearches.incrementAndGet();
        if (cacheHit) {
            cacheHits.incrementAndGet();
        }
        responseTimes.addLast(durationNanos);
        // 保留最近 1000 条记录
        while (responseTimes.size() > 1000) {
            responseTimes.pollFirst();
        }
    }

    /**
     * 获取搜索性能统计快照
     */
    public SearchPerfStats getStats() {
        long total = totalSearches.get();
        long hits = cacheHits.get();
        double hitRate = total > 0 ? (double) hits / total * 100 : 0;

        long p99 = 0L;
        long avg = 0L;
        if (!responseTimes.isEmpty()) {
            var sorted = responseTimes.stream().sorted().toList();
            int p99Index = (int) Math.ceil(sorted.size() * 0.99) - 1;
            p99 = sorted.get(Math.max(0, p99Index));
            avg = (long) sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        return new SearchPerfStats(total, hits, hitRate, avg / 1_000_000, p99 / 1_000_000);
    }

    /**
     * 搜索性能统计快照
     *
     * @param totalSearches 总搜索次数
     * @param cacheHits     缓存命中次数
     * @param cacheHitRate  缓存命中率（百分比）
     * @param avgMs         平均耗时（毫秒）
     * @param p99Ms         P99 耗时（毫秒）
     */
    public record SearchPerfStats(
            long totalSearches,
            long cacheHits,
            double cacheHitRate,
            long avgMs,
            long p99Ms
    ) {
    }
}
