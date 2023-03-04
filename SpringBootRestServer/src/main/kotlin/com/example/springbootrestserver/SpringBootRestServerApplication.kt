package com.example.springbootrestserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringBootRestServerApplication

fun main(args: Array<String>) {
    runApplication<SpringBootRestServerApplication>(*args)
}
