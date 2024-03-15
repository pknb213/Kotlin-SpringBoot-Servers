package com.example.spring_multi_module.ports.`in`

import jakarta.validation.constraints.Null

interface DeleteUserUseCase {
    fun delete(id: Long): DeleteUserOutput

    data class DeleteUserOutput(
        val ok: Boolean,
        val error: Error?,
        val data: Null?
    )
}