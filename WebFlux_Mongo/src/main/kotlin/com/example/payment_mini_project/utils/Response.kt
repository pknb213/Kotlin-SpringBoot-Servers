package com.example.payment_mini_project.utils

import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerResponse

object Response {
    fun okResponse() = ServerResponse.ok().body(
        BodyInserters.fromValue(
            mapOf(
                "success" to true,
                "errorMessage" to null
            )
    ))

    fun errorResponse(e: Throwable) = ServerResponse.ok().body(
        BodyInserters.fromValue(
            mapOf(
                "success" to false,
                "errorMessage" to e.message
            )
        ))
}
