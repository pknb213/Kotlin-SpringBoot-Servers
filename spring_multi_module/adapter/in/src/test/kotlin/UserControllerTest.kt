package com.example.spring_multi_module.adapter.`in`

import com.example.spring_multi_module.UserController
import com.example.spring_multi_module.ports.`in`.CreateUserUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureRestDocs
@WebMvcTest(controllers = [UserController::class])
internal class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var createUserUseCase: CreateUserUseCase

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun testCreateUser() {
        val createUserInput = CreateUserUseCase.CreateUserInput(
            "test",
            "test",
            "test",
        )
//        val userResult = CreateUserUseCase.fromDomainEntity(
//
//        )

    }

}