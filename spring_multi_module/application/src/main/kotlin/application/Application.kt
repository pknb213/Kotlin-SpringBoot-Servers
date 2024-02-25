package com.example.application
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
//    for (i in 1..5) {
//        println(i.toString().isNotNumber())
//    }
    System.setProperty("spring.config.name", "application-domain")
    runApplication<Application>(*args)
}