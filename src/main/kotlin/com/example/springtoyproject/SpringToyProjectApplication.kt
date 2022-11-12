package com.example.springtoyproject

import com.example.springtoyproject.common.MongoUtil
import org.bson.Document
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Random

@SpringBootApplication
class SpringToyProjectApplication

fun main(args: Array<String>) {
    runApplication<SpringToyProjectApplication>(*args)
    println("@#$#@#@$ Main #$@$#$@#")
}

operator fun Model.set(attributeName: String, attributeValue: Any) {
    this.addAttribute(attributeName, attributeValue)
}

@RestController
class MessageResource {
    @GetMapping
    fun index(): List<Message> = listOf(
        Message("1", "Hello!"),
        Message("2", "Bonjour!"),
        Message("3", "Privet!"),
    )
}

@Controller
class Controller {
    @GetMapping("/model")
    fun blog(model: Model): String{
        model["title"] = "Blog"
        return "blog"
    }

    @GetMapping("/mongo")
    fun db(): String {
        return Flux.from(MongoUtil.getCollection("test").find())
            .collectList().map { it }.toString()
    }

    @PostMapping("/data")
    fun post(): Mono<Map<String, Int>> {
        val random = Random()
        val num = random.nextInt(100)
        return Mono.from(MongoUtil.getCollection("test").insertOne(
            Document("data", num)
        )).map { println(it); mapOf("data" to num) }
    }
}


data class Message(val id: String?, val text: String)