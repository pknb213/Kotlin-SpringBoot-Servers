package com.example.springboot_by_kotlin.handler

import com.example.springboot_by_kotlin.global.kafka.KafkaProducer
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Component
class SampleHandler(
    private val kafkaProducer: KafkaProducer
) {
    suspend fun test(req: ServerRequest): ServerResponse {
        val topic = req.pathVariable("topic-test")
        val msg = req.awaitBody<String>()
        println("\n Topic: $topic Msg: $msg")
        kafkaProducer.send(topic, msg)
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(mapOf("Topic" to topic, "Msg" to msg))
    }
}