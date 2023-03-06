package com.example.springbootrestserver.domain.user.domain

import com.example.springbootrestserver.domain.user.dto.UserDto
import com.example.springbootrestserver.global.common.CoreEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

enum class UserRole {
    Client,
    Owner,
    Delivery
}

@Table
class User(
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val verified: Boolean
): CoreEntity()

fun User.toDto(): UserDto = UserDto(
    name = name,
    email = email,
    password = password,
    role = role,
    verified = verified
)