package com.example.payment_mini_project.handler

import com.example.payment_mini_project.domain.group.Group
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
class GroupHandlerTest {
    @Autowired
    protected var webTestClient: WebTestClient? = null

    @Test
    @DisplayName("POST /group을 수행한다")
    fun insertGroup() {
        webTestClient!!.post().uri("/group")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                BodyInserters.fromValue(
                    Group(
                        groupId = 1,
                        description = "Test 중",
                        condition = listOf(
                            "[{key: residence, operator: equals, value: 제주}]",
                            "[{key: amount, operator: lessThan, value: 10000},{key: methodType, operator: equals, value: 카울}]",
                            "[{key: region, operator: in, value: [서울, 경기]},{key: itemCategory, operator: equals, value: 패션},{key: age, operator: between, value: [30, 39]}]",
                            "[{key: methodType, operator: equals, value: SEND},{key: residence, operator: not equals, value: \$region}]"
                        ).random()
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
    @DisplayName("GET /group을 수행한다")
    fun findGroup() {
        webTestClient!!.get().uri("/group")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @DisplayName("DELETE /group을 수행한다. 1번 그룹아이디를 제거한다.")
    fun resetAccountAllData() {
        webTestClient!!.delete().uri("/payment?groupId=1")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @DisplayName("GET /statistics을 수행한다. 1번 그룹아이디를 조건으로 통계 수행.")
    fun statisticsGroup() {
        webTestClient!!.get().uri("/statistics?groupId=1")
            .exchange()
            .expectStatus()
            .isOk
    }
}