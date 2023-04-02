package com.example.springbootrestserver.domain.user.dto

data class SignUpDto (
    val email: String,
    val password: String,
    val nickname: String,
)
