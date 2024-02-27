package com.example.spring_multi_module.domain.entitys.user

enum class UserRole(
    val id: Long,
    val description: String,
) {
    ADMIN(0L, "관리자"),
    COMMON(1L, "일반유저"),
}