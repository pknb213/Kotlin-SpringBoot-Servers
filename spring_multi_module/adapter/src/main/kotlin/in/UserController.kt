package com.example.`in`

import com.example.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/users")
    fun createUser(): ResponseEntity<*> {
        println("Create User API")
        val user = userService.createUser()
        println(user)
        return ResponseEntity.ok("success")
    }
}