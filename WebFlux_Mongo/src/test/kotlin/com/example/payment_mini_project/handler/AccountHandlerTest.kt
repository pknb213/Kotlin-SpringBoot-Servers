package com.example.payment_mini_project.handler

import com.example.payment_mini_project.domain.account.Account
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import kotlin.random.Random

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@AutoConfigureWebTestClient(timeout = "10000")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountHandlerTest {
    @Autowired
    protected var webTestClient: WebTestClient? = null

    @Test
    @DisplayName("POST /account을 수행한다")
    fun insertAccount() {
        webTestClient!!.post().uri("/account")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                Account(
                    accountId = Math.random().hashCode(),
                    residence = listOf("서울", "부산", "경기", "인천", "전북", "전남", "경북", "경남").random(),
                    age = (1..80).random()
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
    @DisplayName("DELETE /account/reset을 수행한다")
    fun resetAccountAllData() {
        webTestClient!!.delete().uri("/account/reset")
            .exchange()
            .expectStatus()
            .isOk
    }
}
