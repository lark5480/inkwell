package com.blog.schedule;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.blog.repository.ArticleViewHistoryRepository;

/**
 * 每天凌晨3点定期清理三个月前的浏览历史
 */
@Component
public class ViewHistoryCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ViewHistoryCleanupTask.class);

    private final ArticleViewHistoryRepository viewHistoryRepository;

    public ViewHistoryCleanupTask(ArticleViewHistoryRepository viewHistoryRepository) {
        this.viewHistoryRepository = viewHistoryRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨3点
    public void cleanupOldHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(3);
        int deleted = viewHistoryRepository.deleteByCreateTimeBefore(cutoff);
        log.info("清理 {} 之前的浏览历史，共删除 {} 条", cutoff, deleted);
    }
}
