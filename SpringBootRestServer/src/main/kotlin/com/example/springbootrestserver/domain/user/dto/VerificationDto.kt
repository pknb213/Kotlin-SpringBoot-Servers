package com.example.springbootrestserver.domain.user.dto

import com.example.springbootrestserver.domain.user.domain.User
import com.example.springbootrestserver.domain.user.domain.Verification

data class VerificationDto (
    val code: String,
    val user: User
)

fun VerificationDto.toEntity(): Verification = Verification(
    code = code,
    user = user
)