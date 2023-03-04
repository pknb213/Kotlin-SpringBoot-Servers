package com.example.springbootrestserver

import com.example.springbootrestserver.handler.UserHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

class Routers (
    private val userHandler: UserHandler
){
    @Bean
    fun userRouter() = coRouter {
        "/api/user".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("", userHandler::getAll)
                POST("", userHandler::getAll)
                "/{id}".nest {
                    GET("", userHandler::getAll)
                    PUT("", userHandler::getAll)
                    DELETE("", userHandler::getAll)
                }
            }
        }
    }
}