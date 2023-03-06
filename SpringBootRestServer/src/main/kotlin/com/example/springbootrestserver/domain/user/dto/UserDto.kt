package com.example.springbootrestserver.domain.user.dto

import com.example.springbootrestserver.domain.user.domain.User
import com.example.springbootrestserver.domain.user.domain.UserRole

data class UserDto (
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val verified: Boolean
)

fun UserDto.toEntity(): User = User(
    name = name,
    email = email,
    password = password,
    role = role,
    verified = verified
)