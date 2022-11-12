package com.example.payment_mini_project.handler

import com.example.payment_mini_project.domain.account.Account
import com.example.payment_mini_project.domain.payment.Payment
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@AutoConfigureWebTestClient(timeout = "10000")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentHandlerTest {
    @Autowired
    protected var webTestClient: WebTestClient? = null

    @Test
    @DisplayName("POST /payment 수행한다")
    fun insertPayment() {
        webTestClient!!.post().uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                BodyInserters.fromValue(
                Payment(
                    paymentId = Math.random().hashCode(),
                    accountId = Math.random().hashCode(),
                    itemCategory = listOf("패션", "식품", "뷰티", "스포츠", "도서").random(),
                    region = listOf("서울", "부산", "경기", "인천", "전북", "전남", "경북", "경남").random(),
                    amount = (0..100000).random(),
                    methodType = listOf("카드", "송금").random()
                )
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo("true")
            .jsonPath("$.errorMessage").isEmpty()
    }

    @Test
    @DisplayName("DELETE /payment/reset을 수행한다")
    fun resetAccountAllData() {
        webTestClient!!.delete().uri("/payment/reset")
            .exchange()
            .expectStatus()
            .isOk
    }
}