-- Kafka事件日志表（用于幂等性保证）
-- 防止重复消费Kafka消息

CREATE TABLE IF NOT EXISTS `kafka_event_log` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `event_type` VARCHAR(100) NOT NULL COMMENT '事件类型：OrderCreated/OrderPaid',
    `event_key` VARCHAR(200) NOT NULL COMMENT '事件唯一标识（如订单ID）',
    `event_payload` TEXT COMMENT '原始消息内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING/SUCCESS/FAILED',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `error_message` TEXT COMMENT '错误信息',
    `processed_time` DATETIME COMMENT '处理完成时间',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_type_key` (`event_type`, `event_key`),
    INDEX `idx_event_type` (`event_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kafka事件日志表';

-- 示例数据（可选）
-- INSERT INTO `kafka_event_log` (`event_type`, `event_key`, `event_payload`, `status`, `created_time`) VALUES
-- ('OrderPaid', '1001', '{"orderId":1001,...}', 'SUCCESS', NOW());
