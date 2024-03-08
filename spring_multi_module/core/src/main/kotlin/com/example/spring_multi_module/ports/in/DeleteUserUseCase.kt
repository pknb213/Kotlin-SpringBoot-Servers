package com.example.spring_multi_module.ports.`in`

interface DeleteUserUseCase {
    fun deleteUser(deleteUserInput: DeleteUserInput): DeleteUserOutput

    data class DeleteUserInput(
        val id: Long
    )

    data class DeleteUserOutput(
        val ok: Boolean,
        val error: Error?,
        val data: Any
    )
}