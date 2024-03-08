package com.example.spring_multi_module.domain

import com.example.spring_multi_module.domain.entitys.com.example.spring_multi_module.entitys.user.User
import com.example.spring_multi_module.domain.entitys.com.example.spring_multi_module.entitys.user.UserRole

object UserTestFixture {
    const val USER_ID = 1L
    const val USER_NAME = "test"
    const val USER_EMAIL = "test@google.com"
    const val USER_PASSWORD = "1234"

    fun createUser(
        id: Long = USER_ID,
        name: String = USER_NAME,
        email: String = USER_EMAIL,
        password: String = USER_PASSWORD
    ) = User(
        id,
        name,
        email,
        password,
        UserRole.COMMON
    )
}