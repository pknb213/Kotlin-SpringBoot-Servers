package com.example.springbootrestserver

import com.example.springbootrestserver.domain.user.handler.UserHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.coRouter

@Component
class Routers (
    private val userHandler: UserHandler
){
    @Bean
    fun userRouter() = coRouter {
        "/api/user".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("", userHandler::getAll)
                POST("", userHandler::postOne)
                "/{id}".nest {
                    GET("", userHandler::getOne)
                    PUT("", userHandler::putOne)
                    DELETE("", userHandler::deleteOne)
                }
            }
        }
    }
    @Bean
    fun boardRouter() = coRouter {
        "/api/board".nest {
            accept(MediaType.APPLICATION_JSON).nest {

            }
        }
    }
}