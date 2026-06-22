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
 * Sensitive word filter for comment auto-moderation.
 * <p>
 * Loads comma-separated sensitive words from the {@code sensitive_words} setting.
 * Words are cached in-memory and refreshed at most once every 5 minutes
 * to avoid hitting the database on every comment.
 * </p>
 * <p>
 * Matching is case-insensitive and covers both subject and object fields.
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
     * Check if the given text contains any sensitive words.
     *
     * @param text the text to check (content, author name, etc.)
     * @return true if any sensitive word is found
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
     * Force a refresh of the cached sensitive word patterns.
     * Called after the admin updates the sensitive_words setting.
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
