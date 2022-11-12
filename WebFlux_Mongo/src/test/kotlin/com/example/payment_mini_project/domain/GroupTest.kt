package com.example.payment_mini_project.domain

import com.example.payment_mini_project.domain.account.Account
import com.example.payment_mini_project.domain.group.Group
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GroupTest {
    @Test
    @DisplayName("정상적으로 Group 객체를 생성한다.")
    fun create() {
        Assertions.assertDoesNotThrow<Any> {
            Group(
                groupId = 1,
                condition = "[{key: region, operation: equals, value: 부산}]",
                description = "부산 결제 내역"
            )
        }
    }
}