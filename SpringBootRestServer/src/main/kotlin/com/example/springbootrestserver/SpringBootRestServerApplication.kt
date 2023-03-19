package com.example.springbootrestserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SpringBootRestServerApplication {

}

fun main(args: Array<String>) {
    runApplication<SpringBootRestServerApplication>(*args)
}
