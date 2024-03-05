package com.example.spring_multi_module.adapter.`in`

import org.springframework.web.bind.annotation.GetMapping
import com.example.spring_multi_module.core.ports.`in`.CreateUserUseCase
import com.example.spring_multi_module.core.ports.`in`.GetAllUserUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController(
    private val createUserUseCase: CreateUserUseCase,
    private val getAllUserUseCase: GetAllUserUseCase
) {
    @PostMapping
    fun createUser(
        @RequestBody createUserInput: CreateUserUseCase.CreateUserInput
    ): CreateUserUseCase.CreateUserOutput {
        println("Create User API")
        val (email, password, name) = createUserInput
        return createUserUseCase.createUser(
            CreateUserUseCase.CreateUserInput(
                email = email,
                password = password,
                name = name
            )
        )
    }

    @GetMapping
    fun getAllUser(): GetAllUserUseCase.GetAllUserOutput {
        println("Get All User API")
        return getAllUserUseCase.getAllUser()
    }
}