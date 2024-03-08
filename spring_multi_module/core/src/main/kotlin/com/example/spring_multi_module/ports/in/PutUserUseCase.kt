package com.example.spring_multi_module.ports.`in`

interface PutUserUseCase {
    fun putUser(putUserInput: PutUserInput): PutUserOutput

    data class PutUserInput(
        val id: Long,
    ) {

    }

    data class PutUserOutput(
        val ok: Boolean,
        val error: Error?,
        val data: Any
    ) {

    }
}