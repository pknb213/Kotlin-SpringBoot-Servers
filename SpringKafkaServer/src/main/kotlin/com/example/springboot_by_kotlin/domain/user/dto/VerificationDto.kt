package com.example.springboot_by_kotlin.domain.user.dto

import com.example.springboot_by_kotlin.domain.user.domain.User
import com.example.springboot_by_kotlin.domain.user.domain.Verification

data class VerificationDto (
    val code: String,
    val user: User
)

fun VerificationDto.toEntity(): Verification = Verification(
    code = code,
    user = user
)