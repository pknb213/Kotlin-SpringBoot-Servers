package com.example.spring_multi_module.adapter.`in`

import com.example.spring_multi_module.core.ports.`in`.CreateUserUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController(
        private val createUserUseCase: CreateUserUseCase
) {
    @GetMapping
    fun createUser(): CreateUserUseCase.CreateUserOutput {
        println("Create User API")
        return createUserUseCase.createUser(
            CreateUserUseCase.CreateUserInput(
                "youngjo@dong-a.com",
                "1234",
                "YoungJo")
        )
    }
}