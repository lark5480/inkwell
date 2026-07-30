package com.blog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchPerfCollectorTest {

    private SearchPerfCollector collector;

    @BeforeEach
    void setUp() {
        collector = new SearchPerfCollector();
    }

    @Test
    void getStats_empty_shouldReturnZeros() {
        var stats = collector.getStats();
        assertEquals(0, stats.totalSearches());
        assertEquals(0, stats.cacheHits());
        assertEquals(0.0, stats.cacheHitRate());
        assertEquals(0, stats.avgMs());
        assertEquals(0, stats.p99Ms());
    }

    @Test
    void recordSearch_shouldTrackTotals() {
        collector.recordSearch(1_000_000, false); // 1ms
        collector.recordSearch(2_000_000, true);  // 2ms, cache hit
        collector.recordSearch(3_000_000, false); // 3ms

        var stats = collector.getStats();
        assertEquals(3, stats.totalSearches());
        assertEquals(1, stats.cacheHits());
        assertEquals(33.33, stats.cacheHitRate(), 0.1);
    }

    @Test
    void recordSearch_p99Calculation() {
        // Record 100 entries with latency from 1ms to 100ms
        for (int i = 1; i <= 100; i++) {
            collector.recordSearch(i * 1_000_000L, false);
        }
        var stats = collector.getStats();
        // P99 index = ceil(100 * 0.99) - 1 = 98, sorted[98] = 99ms
        assertEquals(99, stats.p99Ms());
    }

    @Test
    void recordSearch_avgCalculation() {
        // Record 100 entries with latency from 1ms to 100ms
        for (int i = 1; i <= 100; i++) {
            collector.recordSearch(i * 1_000_000L, false);
        }
        var stats = collector.getStats();
        // avg = (1+2+...+100)/100 = 50.5ms, cast to long = 50
        assertEquals(50, stats.avgMs());
    }

    @Test
    void recordSearch_maxSize1000() {
        // Record 1500 entries, should only keep the most recent 1000
        for (int i = 0; i < 1500; i++) {
            collector.recordSearch(1_000_000, false);
        }
        var stats = collector.getStats();
        assertEquals(1500, stats.totalSearches()); // total count is still 1500
        // P99 is based on the most recent 1000 entries (all 1ms), so p99 = 1ms
        assertEquals(1, stats.p99Ms());
    }

    @Test
    void recordSearch_cacheHitCounting() {
        collector.recordSearch(1_000_000, true);
        collector.recordSearch(1_000_000, true);
        collector.recordSearch(1_000_000, false);
        collector.recordSearch(1_000_000, true);

        var stats = collector.getStats();
        assertEquals(4, stats.totalSearches());
        assertEquals(3, stats.cacheHits());
        assertEquals(75.0, stats.cacheHitRate(), 0.01);
    }
}
