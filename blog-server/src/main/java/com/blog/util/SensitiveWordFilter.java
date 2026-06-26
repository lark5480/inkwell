package com.blog.util;

import com.blog.repository.SettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 敏感词过滤器，用于评论自动审核。
 * <p>
 * 从 {@code sensitive_words} 配置项中加载由逗号分隔的敏感词列表。
 * 编译后的正则表达式缓存在内存中，每 5 分钟刷新一次，
 * 避免每次评论都查询数据库。
 * </p>
 * <p>
 * 匹配不区分大小写，同时覆盖内容和作者字段。
 * </p>
 */
@Component
public class SensitiveWordFilter {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordFilter.class);
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes

    private final SettingRepository settingRepository;

    private volatile List<Pattern> cachedPatterns = Collections.emptyList();
    private volatile LocalDateTime lastRefresh = LocalDateTime.MIN;

    private static final String SETTING_KEY = "sensitive_words";

    public SensitiveWordFilter(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * 检查文本是否包含敏感词。
     *
     * @param text 待检查的文本（内容、作者名等）
     * @return 包含敏感词返回 true
     */
    public boolean containsSensitiveWords(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        List<Pattern> patterns = getPatterns();
        String normalized = text.toLowerCase();
        for (Pattern p : patterns) {
            if (p.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 强制刷新敏感词缓存。
     * 管理员修改敏感词配置后调用，使新配置即时生效。
     */
    public void refresh() {
        lastRefresh = LocalDateTime.MIN;
        getPatterns();
    }

    private List<Pattern> getPatterns() {
        if (cachedPatterns.isEmpty()
                || lastRefresh.plusSeconds(CACHE_TTL_SECONDS).isBefore(LocalDateTime.now())) {
            reloadPatterns();
        }
        return cachedPatterns;
    }

    private synchronized void reloadPatterns() {
        // Double-check after acquiring lock
        if (!cachedPatterns.isEmpty()
                && lastRefresh.plusSeconds(CACHE_TTL_SECONDS).isAfter(LocalDateTime.now())) {
            return;
        }

        List<Pattern> patterns;
        try {
            String raw = settingRepository.findBySettingKey(SETTING_KEY)
                    .map(s -> s.getSettingValue())
                    .orElse("");

            if (raw.isBlank()) {
                patterns = Collections.emptyList();
            } else {
                patterns = Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(w -> !w.isBlank())
                        .map(w -> Pattern.compile(Pattern.quote(w), Pattern.CASE_INSENSITIVE))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Failed to load sensitive words, using cached patterns", e);
            return; // keep existing cache on error
        }

        cachedPatterns = patterns;
        lastRefresh = LocalDateTime.now();
        log.info("Loaded {} sensitive word patterns", patterns.size());
    }
}
