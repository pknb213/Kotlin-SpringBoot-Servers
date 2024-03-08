package com.example.spring_multi_module.ports.services

import com.example.spring_multi_module.ports.`in`.DeleteUserUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DeleteUserUseCaseImpl (
//    private val deleteUserPort: DeleteUserPort
): DeleteUserUseCase {
    @Transactional
    override fun deleteUser(deleteUserInput: DeleteUserUseCase.DeleteUserInput): DeleteUserUseCase.DeleteUserOutput {
        TODO("Not yet implemented")
    }
}