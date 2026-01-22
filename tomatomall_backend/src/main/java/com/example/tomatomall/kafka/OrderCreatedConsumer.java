package com.example.tomatomall.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    @KafkaListener(topics = "tomatomall.order.created")
    public void onMessage(String message) {
        System.out.println("Kafka received tomatomall.order.created: " + message);
    }
}

