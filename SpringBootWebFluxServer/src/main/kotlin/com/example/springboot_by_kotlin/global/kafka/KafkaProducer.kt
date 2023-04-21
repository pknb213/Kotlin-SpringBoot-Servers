package com.example.springboot_by_kotlin.global.kafka

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) {
    fun send(topic: String, message: String) {
        kafkaTemplate.send(topic, message)
    }
}