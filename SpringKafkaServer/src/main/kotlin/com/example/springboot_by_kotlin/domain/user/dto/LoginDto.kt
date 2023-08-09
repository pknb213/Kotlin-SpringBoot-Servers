package com.example.springboot_by_kotlin.domain.user.dto

data class LoginDto (
    val email: String,
    val password: String
)

//fun LoginDto.toEntity(): LoginDto = LoginDto(
//    email = email,
//    password = password,
//)