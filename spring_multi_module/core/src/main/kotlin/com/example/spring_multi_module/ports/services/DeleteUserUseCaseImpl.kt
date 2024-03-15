package com.example.spring_multi_module.ports.services

import com.example.spring_multi_module.ports.`in`.DeleteUserUseCase
import com.example.spring_multi_module.ports.out.DeleteUserPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DeleteUserUseCaseImpl (
    private val deleteUserPort: DeleteUserPort
): DeleteUserUseCase {
    @Transactional
    override fun delete(id: Long): DeleteUserUseCase.DeleteUserOutput {
        deleteUserPort.deleteUser(id)
        return DeleteUserUseCase.DeleteUserOutput(
            ok = true,
            error = null,
            data = null)
    }
}