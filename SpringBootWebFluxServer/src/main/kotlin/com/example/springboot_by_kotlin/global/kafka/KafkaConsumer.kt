package com.example.springboot_by_kotlin.global.kafka

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaConsumer {
    @KafkaListener(topics = ["test-topic"], groupId = "test-group")
    fun receive(message: String) {
        println("Received message: $message")
    }
}