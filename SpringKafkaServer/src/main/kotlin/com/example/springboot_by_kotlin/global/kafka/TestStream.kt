package com.example.springboot_by_kotlin.global.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class TestStream {
//    private val leveragePriceSerde = SpecificAvroSerde<>(
    private val testStream = "/com/example/springboot_by_kotlin/avro/test-quote.avsc"
//    val serdeConfig: MutableMap<String, String> = Collections.singletonMap(
//        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL
//    )
    @PostConstruct
    fun init() {
//        leveragePriceSerde.configure(serdeConfig, false)
    }
}