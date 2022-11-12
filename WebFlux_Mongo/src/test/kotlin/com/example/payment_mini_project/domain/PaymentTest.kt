package com.example.payment_mini_project.domain

import com.example.payment_mini_project.domain.payment.Payment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PaymentTest {
    @Test
    @DisplayName("정상적으로 Payment 객체를 생성한다.")
    fun create() {
        Assertions.assertDoesNotThrow<Any> {
            Payment(
                paymentId = 1,
                amount = 1000,
                region = "서울",
                accountId = 1,
                methodType = "CARD",
                itemCategory = "FASHION"
            )
        }
    }
}