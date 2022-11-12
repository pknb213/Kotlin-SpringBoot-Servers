package com.example.payment_mini_project.domain

import com.example.payment_mini_project.domain.account.Account
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AccountTest {
    @Test
    @DisplayName("정상적으로 Account 객체를 생성한다.")
    fun create() {
        Assertions.assertDoesNotThrow<Any> {
            Account(
                accountId = 1,
                age = 21,
                residence = "서울"
            )
        }
    }
}