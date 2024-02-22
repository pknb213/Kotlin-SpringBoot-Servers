package com.example

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/api/user")
    fun createUser(): ResponseEntity<*> {
        userService.createUser()
        return ResponseEntity.ok("success")
    }
}