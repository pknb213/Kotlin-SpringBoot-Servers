package com.example.spring_multi_module.ports.services

import org.springframework.transaction.annotation.Transactional
import com.example.spring_multi_module.ports.`in`.GetAllUserUseCase
import com.example.spring_multi_module.ports.out.GetAllUserPort
import org.springframework.stereotype.Service

@Service
@Transactional(readOnly = true)
class GetAllUserUseCaseImpl(
    private val getAllUserPort: GetAllUserPort
): GetAllUserUseCase {
    @Transactional
    override fun getAllUser(): GetAllUserUseCase.GetAllUserOutput {
//        println("Get Impl")
        val users = getAllUserPort.getAll()
        return GetAllUserUseCase.fromDomain(users)
    }
}