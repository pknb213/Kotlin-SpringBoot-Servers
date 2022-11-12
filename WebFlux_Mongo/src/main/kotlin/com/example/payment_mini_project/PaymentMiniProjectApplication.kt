package com.example.payment_mini_project

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.netty.http.server.HttpServer

@SpringBootApplication
class PaymentMiniProjectApplication

fun main(args: Array<String>) {
	runApplication<PaymentMiniProjectApplication>(*args)
}
