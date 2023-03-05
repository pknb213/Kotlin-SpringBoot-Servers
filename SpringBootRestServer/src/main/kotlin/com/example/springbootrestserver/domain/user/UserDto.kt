package com.example.springbootrestserver.domain.user

data class UserDto (
    val name: String
)

fun UserDto.toEntity(): User = User(
    name = name
)