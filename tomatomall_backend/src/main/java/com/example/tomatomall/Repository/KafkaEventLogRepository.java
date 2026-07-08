package com.example.tomatomall.Repository;

import com.example.tomatomall.po.KafkaEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KafkaEventLogRepository extends JpaRepository<KafkaEventLog, Integer> {

    Optional<KafkaEventLog> findByEventTypeAndEventKey(String eventType, String eventKey);

    boolean existsByEventTypeAndEventKey(String eventType, String eventKey);
}
