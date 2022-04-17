package com.example.springtoyproject

import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

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
}


data class Message(val id: String?, val text: String)