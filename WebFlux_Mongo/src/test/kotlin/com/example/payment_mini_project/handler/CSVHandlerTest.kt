package com.example.payment_mini_project.handler

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@AutoConfigureWebTestClient(timeout = "10000")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CSVHandlerTest {
    @Autowired
    protected var webTestClient: WebTestClient? = null

    @Test
    @DisplayName("Resource에 존재하는 account.csv 파일을 참조하여 DB에 적재한다.")
    fun insertAccountsFromCsv() {
        webTestClient!!.get().uri("/csv?file=accounts")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @DisplayName("Resource에 존재하는 payments.csv 파일을 참조하여 DB에 적재한다.")
    fun insertPaymentsFromCsv() {
        webTestClient!!.get().uri("/csv?file=payments")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @DisplayName("Resource에 존재하는 groups.csv 파일을 참조하여 DB에 적재한다.")
    fun insertGroupsFromCsv() {
        webTestClient!!.get().uri("/csv?file=groups")
            .exchange()
            .expectStatus()
            .isOk
    }
}