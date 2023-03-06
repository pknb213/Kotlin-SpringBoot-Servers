package com.example.springbootrestserver.domain.user.domain

import com.example.springbootrestserver.domain.user.dto.VerificationDto
import com.example.springbootrestserver.global.common.CoreEntity
import org.springframework.data.relational.core.mapping.Table

@Table
class Verification(
    val code: String,
    val user: User
): CoreEntity()

fun Verification.toDto(): VerificationDto = VerificationDto(
    code = code,
    user = user
)