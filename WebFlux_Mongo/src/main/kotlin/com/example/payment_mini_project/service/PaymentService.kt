package com.example.payment_mini_project.service

import com.example.payment_mini_project.domain.payment.Payment
import com.example.payment_mini_project.domain.payment.PaymentRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PaymentService (private val paymentRepository: PaymentRepository){
    fun insert(payment: Payment): Mono<Payment> = paymentRepository.insert(payment)
    fun deleteAll(): Mono<Void> = paymentRepository.deleteAll()
}