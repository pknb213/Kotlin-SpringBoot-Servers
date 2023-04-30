import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component
class TestTopology {
    @Autowired
    fun buildTopology(inputTopic: String?, outputTopic: String?): Topology? {
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
    companion object {
        private val STRING_SERDE = Serdes.String()
    }
}