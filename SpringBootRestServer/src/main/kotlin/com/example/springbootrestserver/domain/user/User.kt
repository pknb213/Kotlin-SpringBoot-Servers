package com.example.springbootrestserver.domain.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table
class User(
    @Id var id: Long? = null,
    val name: String,
    val created_date: LocalDateTime = LocalDateTime.now(),
    val updated_date: LocalDateTime = LocalDateTime.now()
)

fun User.toDto(): UserDto = UserDto(
    name = name
)