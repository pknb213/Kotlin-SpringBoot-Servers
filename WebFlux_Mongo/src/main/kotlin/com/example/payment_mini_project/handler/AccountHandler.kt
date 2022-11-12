package com.example.payment_mini_project.handler

import com.example.payment_mini_project.domain.account.Account
import com.example.payment_mini_project.domain.account.AccountRepository
import com.example.payment_mini_project.domain.payment.Payment
import com.example.payment_mini_project.service.AccountService
import com.example.payment_mini_project.utils.Response
import org.springframework.context.annotation.ComponentScan
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class AccountHandler (private val accountService: AccountService){
    fun insertAccount(request: ServerRequest): Mono<ServerResponse> =
        request.bodyToMono(Account::class.java)
            .flatMap {
                accountService.insert(it)
            }.flatMap {
                Response.okResponse()
            }.onErrorResume {
                Response.errorResponse(it)
            }

    fun resetAccount(request: ServerRequest): Mono<ServerResponse> =
        Mono.just(1).flatMap {
            accountService.deleteAll()
        }.flatMap {
            Response.okResponse()
        }.onErrorResume {
            Response.errorResponse(it)
        }
}