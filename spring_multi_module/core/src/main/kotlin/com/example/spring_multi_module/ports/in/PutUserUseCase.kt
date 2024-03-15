package com.example.spring_multi_module.ports.`in`

import com.example.spring_multi_module.entitys.user.UserRole
import jakarta.validation.constraints.Null


interface PutUserUseCase {
    fun putUser(putUserInput: PutUserInput): PutUserOutput

    data class PutUserInput(
        val email: String?,
        val password: String?,
        val name: String?,
        val roleId: UserRole?
    ) {
        init {

        }
    }

    data class PutUserOutput(
        val ok: Boolean,
        val error: Error?,
        val data: Null?
    ) {
    }
}