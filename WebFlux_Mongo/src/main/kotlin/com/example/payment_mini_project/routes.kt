package com.example.payment_mini_project

import com.example.payment_mini_project.handler.AccountHandler
import com.example.payment_mini_project.handler.CsvFileUploadHandler
import com.example.payment_mini_project.handler.GroupHandler
import com.example.payment_mini_project.handler.PaymentHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.router

@Configuration
class routes(
    private val paymentHandler: PaymentHandler,
    private val accountHandler: AccountHandler,
    private val groupHandler: GroupHandler,
    private val csvFileUploadHandler: CsvFileUploadHandler
) {
    @Bean
    fun csvRouter() = router {
        (accept(MediaType.APPLICATION_JSON))
            .nest {
                GET("/csv", csvFileUploadHandler::insertCsv)
            }
    }

    @Bean
    fun paymentRouter() = router {
        (accept(MediaType.APPLICATION_JSON))
            .nest {
//                GET("/get", paymentHandler::executeExample)
                POST("/payment", paymentHandler::receivePaymentHistory)
                DELETE("/payment/reset", paymentHandler::resetPayment)
            }
    }
    @Bean
    fun accountRouter() = router {
        (accept(MediaType.APPLICATION_JSON))
            .nest {
                POST("/account", accountHandler::insertAccount)
                DELETE("/account/reset", accountHandler::resetAccount)
            }
    }
    @Bean
    fun groupRouter() = router {
        (accept(MediaType.APPLICATION_JSON))
            .nest {
                GET("/group", groupHandler::findGroupData)
                POST("/group", groupHandler::insertGroupData)
                DELETE("/group", groupHandler::deleteGroupData)
                GET("/statistics", groupHandler::findGroupStatisticsData)
            }
    }
}
