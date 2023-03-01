package com.example.springbootserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringBootServerApplication

fun main(args: Array<String>) {
	runApplication<SpringBootServerApplication>(*args)
}
