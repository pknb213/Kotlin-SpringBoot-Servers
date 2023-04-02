package com.example.springbootrestserver.domain.user.dto

data class LoginDto (
    val email: String,
    val password: String
)

fun LoginDto.toEntity(): LoginDto = LoginDto(
    email = email,
    password = password,
)