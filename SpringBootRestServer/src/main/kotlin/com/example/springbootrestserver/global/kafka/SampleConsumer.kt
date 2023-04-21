package com.example.springbootrestserver.global.kafka

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SampleConsumer {
    @KafkaListener(topics = ["test-topic"])
    fun receive(message: String) {
        println("Received message: $message")
    }
}