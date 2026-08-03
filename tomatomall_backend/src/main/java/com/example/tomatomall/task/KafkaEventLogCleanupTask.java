package com.example.tomatomall.task;

import com.example.tomatomall.Repository.KafkaEventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时清理过期的 KafkaEventLog 记录，防止表无限增长。
 */
@Component
public class KafkaEventLogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventLogCleanupTask.class);

    /** 成功事件保留天数 */
    private static final int RETENTION_DAYS = 7;

    @Autowired
    private KafkaEventLogRepository kafkaEventLogRepository;

    /**
     * 每天凌晨 3 点清理 7 天前的成功事件记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldSuccessEvents() {
        try {
            LocalDateTime before = LocalDateTime.now().minusDays(RETENTION_DAYS);
            int deleted = kafkaEventLogRepository.deleteOldSuccessEvents(before);
            if (deleted > 0) {
                log.info("KafkaEventLog 清理完成: 删除 {} 条 {} 天前的成功记录", deleted, RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("KafkaEventLog 清理失败: {}", e.getMessage(), e);
        }
    }
}
