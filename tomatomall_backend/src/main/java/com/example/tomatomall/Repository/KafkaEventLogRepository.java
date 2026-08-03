package com.example.tomatomall.Repository;

import com.example.tomatomall.po.KafkaEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface KafkaEventLogRepository extends JpaRepository<KafkaEventLog, Integer> {

    Optional<KafkaEventLog> findByEventTypeAndEventKey(String eventType, String eventKey);

    boolean existsByEventTypeAndEventKey(String eventType, String eventKey);

    /**
     * 清理 N 天前的成功事件日志，防止表无限增长
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM KafkaEventLog e WHERE e.status = 'SUCCESS' AND e.createdTime < :before")
    int deleteOldSuccessEvents(@Param("before") LocalDateTime before);
}
