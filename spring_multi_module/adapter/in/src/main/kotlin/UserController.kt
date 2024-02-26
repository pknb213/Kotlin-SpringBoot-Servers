package com.example.infrastructur.adapter.`in`

import com.example.infrastructure.application.ports.`in`.CreateUserUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController(
//    private val userService: UserService,
        private val createUserUseCase: CreateUserUseCase
) {
    @GetMapping("/users")
    fun createUser(): CreateUserUseCase.CreateUserOutput {
        println("Create User API")
//        val user = userService.createUser()
//        println(user)
//        return ResponseEntity.ok("success")
        return createUserUseCase.createUser(
            CreateUserUseCase.CreateUserInput("", "", "")
        )
    }
}