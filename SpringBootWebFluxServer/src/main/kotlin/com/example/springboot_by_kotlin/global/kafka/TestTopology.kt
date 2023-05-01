import com.example.springboot_by_kotlin.global.kafka.KafkaProducer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.buildAndAwait


@Component
class TestTopology(
    private val kafkaProducer: KafkaProducer
) {
    @Autowired
    suspend fun buildTopology(inputTopic: String?, outputTopic: String?): Topology? {
        val stringSerde: Serde<String> = Serdes.String()
        val builder = StreamsBuilder()
        builder
            .stream(inputTopic, Consumed.with(stringSerde, stringSerde))
            .peek { k, v -> println("Observed event: $v") }
            .mapValues { s -> s.toUpperCase() }
            .peek { k, v -> println("Transformed event: $v") }
            .to(outputTopic, Produced.with(stringSerde, stringSerde))
        return builder.build()
    }
    suspend fun test(req: ServerRequest): ServerResponse {
        val topic = req.pathVariable("test-topic")
        val msg = req.awaitBody<String>()
        println("\n Topic: $topic Msg: $msg")
        val topology = this.buildTopology(topic, topic)
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .buildAndAwait()
//            .bodyValueAndAwait("Input Topic: $topic")
    }
}