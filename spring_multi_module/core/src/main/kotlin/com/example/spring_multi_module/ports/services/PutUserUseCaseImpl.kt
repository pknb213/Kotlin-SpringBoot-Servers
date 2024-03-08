package com.example.spring_multi_module.ports.services

import com.example.spring_multi_module.ports.`in`.PutUserUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PutUserUseCaseImpl (
//    private val putUserPort: PutUserPort
):PutUserUseCase  {
    @Transactional
    override fun putUser(putUserInput: PutUserUseCase.PutUserInput): PutUserUseCase.PutUserOutput {
        TODO("Not yet implemented")
    }
}