package com.example.springbootrestserver

import com.example.springbootrestserver.domain.user.handler.UserHandler
import com.example.springbootrestserver.handler.CityHandler
import com.example.springbootrestserver.handler.StatisticHandler
import com.example.springbootrestserver.handler.TravelHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.coRouter
@Component
class Routes (
    private val cityHandler: CityHandler,
    private val travelHandler: TravelHandler,
    private val statisticHandler: StatisticHandler,
    private val userHandler: UserHandler,
){
    @Bean
    fun pingRouter() = coRouter {
        "/v1/ping".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("", userHandler::ping)
            }
        }
        "/v1/login".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                POST("", userHandler::login)
            }
        }
//        "/v1/kafka/{test-topic}".nest {
//            accept(MediaType.APPLICATION_JSON).nest {
//                POST("", kafkaHandler::test)
//            }
//        }
    }
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