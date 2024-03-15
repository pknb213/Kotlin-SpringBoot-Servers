package com.example.spring_multi_module.ports.services

import com.example.spring_multi_module.entitys.user.UserRole
import com.example.spring_multi_module.ports.`in`.PutUserUseCase
import com.example.spring_multi_module.ports.out.PutUserPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PutUserUseCaseImpl (
    private val putUserPort: PutUserPort
):PutUserUseCase  {
    @Transactional
    override fun putUser(
        putUserInput: PutUserUseCase.PutUserInput
    ): PutUserUseCase.PutUserOutput {
        putUserPort.put(
            name = putUserInput.name,
            email = putUserInput.email,
            password = putUserInput.password,
            roleId = putUserInput.roleId ?: UserRole.COMMON
        )
        return PutUserUseCase.PutUserOutput(
            ok = true,
            error = null,
            data = null
        )
    }
}