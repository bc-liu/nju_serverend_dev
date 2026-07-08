package com.example.tomatomall.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kafka_event_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_type", "event_key"})
})
public class KafkaEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_key", nullable = false, length = 200)
    private String eventKey;

    @Column(name = "event_payload", columnDefinition = "TEXT")
    private String eventPayload;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PROCESSING";

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_time")
    private LocalDateTime processedTime;

    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime = LocalDateTime.now();

    @Column(name = "updated_time")
    private LocalDateTime updatedTime = LocalDateTime.now();

    public KafkaEventLog(String eventType, String eventKey, String eventPayload) {
        this.eventType = eventType;
        this.eventKey = eventKey;
        this.eventPayload = eventPayload;
    }

    public void markAsSuccess() {
        this.status = "SUCCESS";
        this.processedTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.retryCount++;
        this.updatedTime = LocalDateTime.now();
    }
}
