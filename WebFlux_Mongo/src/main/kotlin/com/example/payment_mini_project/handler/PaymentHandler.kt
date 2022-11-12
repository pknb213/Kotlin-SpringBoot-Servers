package com.example.payment_mini_project.handler

import com.example.payment_mini_project.domain.payment.Payment
import com.example.payment_mini_project.service.PaymentService
import com.example.payment_mini_project.utils.Response
import org.springframework.context.annotation.ComponentScan
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono

/**
 * 아래 처럼 lass PaymentHandler (val paymentRepository: PaymentRepository)
 * 형식으로 클래스 필드를 통한 의존성을 권장사항이 아니고 순환 참조 에러를 발생할 수 있다.
 * 클래스 대신 함수에서 의존성을 부여 하라.
 */

//@ComponentScan(basePackages = arrayOf("com.example.payment_mini_project"))
@Component
class PaymentHandler (private val paymentService: PaymentService){
//    fun executeExample(request: ServerRequest): Mono<ServerResponse> {
//        val accountId = request.queryParam("paymentName").get()
//        val amount = request.queryParam("paymentName").get()
//        val methodType = request.queryParam("paymentName").get()
//        val itemCategory = request.queryParam("paymentName").get()
//        val region = request.queryParam("paymentName").get()
//        return ServerResponse.ok().body(BodyInserters.fromObject(
//            Payment(
//                accountId = accountId.toInt(),
//                amount = amount.toInt(),
//                methodType = methodType,
//                itemCategory = itemCategory,
//                region = region
//            )
//        ))
//    }
    fun receivePaymentHistory(request: ServerRequest): Mono<ServerResponse> =
        request.bodyToMono(Payment::class.java)
            .flatMap {
                paymentService.insert(it)
            }.flatMap {
                Response.okResponse()
            }.onErrorResume {
                Response.errorResponse(it)
            }

    fun resetPayment(request: ServerRequest): Mono<ServerResponse> =
        Mono.just(1).flatMap {
            paymentService.deleteAll()
        }.flatMap {
            Response.okResponse()
        }.onErrorResume {
            Response.errorResponse(it)
        }
}