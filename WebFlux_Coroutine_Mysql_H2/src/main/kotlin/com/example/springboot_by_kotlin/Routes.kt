package com.example.springboot_by_kotlin

import com.example.springboot_by_kotlin.handler.CityHandler
import com.example.springboot_by_kotlin.handler.StatisticHandler
import com.example.springboot_by_kotlin.handler.TravelHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.coRouter

@Component
class Routes (
    private val cityHandler: CityHandler,
    private val travelHandler: TravelHandler,
    private val statisticHandler: StatisticHandler
){
    @Bean
    fun cityRouter() = coRouter {
        "/api/citys".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                POST("", cityHandler::add)
                GET("", cityHandler::getAll)
                "/{id}".nest {
                    GET("", cityHandler::getById)
                    PUT("", cityHandler::update)
                    DELETE("", cityHandler::delete)
                }
                "/by/users".nest {
                    GET("", cityHandler::getByCustom)  // 사용자별 도시 목록 조회
                }
            }
        }
    }
    @Bean
    fun travelRouter() = coRouter {
        "/api/travels".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                POST("", travelHandler::add)
                GET("", travelHandler::getAll)
                "/{id}".nest {
                    GET("", travelHandler::getById)
                    PUT("", travelHandler::update)
                    DELETE("", travelHandler::delete)
                }
            }
        }
    }
}
