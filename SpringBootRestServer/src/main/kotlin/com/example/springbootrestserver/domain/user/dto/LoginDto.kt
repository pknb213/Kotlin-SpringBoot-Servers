package com.example.springbootrestserver.domain.user.dto

class LoginDto (
    val email: String,
    val password: String
)

//fun LoginDto.toEntity(): User = User(
//    email = email,
//    password = password,
//)