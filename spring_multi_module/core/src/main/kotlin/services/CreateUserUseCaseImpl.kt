package com.example.spring_multi_module.core.services

import com.example.spring_multi_module.core.ports.`in`.CreateUserUseCase
import com.example.spring_multi_module.core.ports.out.CreateUserPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Incoming port Usecase의 구현체이다.
 * 기본적으로 rich domain model을 선호하여 비지니스 규칙은 usecase 구현체가 아닌 domain entity 안에서 구현하도록 한다.
 * Domain entity 안에서 비지니스 규칙을 검사하기 어려운 경우에는 여기 usecase 구현체에서 진행한다.
 */
@Service
/**
 * @Transactional은 web이나 persistence가 아니라 service 계층에서 사용하여야 한다.
 * Service 계층만이 transaction boudary를 결정할 수 있다.
 * 트랜잭션이 필요한 클래스에는 readOnly = true를 붙이고 DML이 발생하는 method에만 @Transactional을 추가로 붙인다.
 * - https://vladmihalcea.com/spring-transactional-annotation/
 */
@Transactional(readOnly = true)
class CreateUserUseCaseImpl (  // Todo: CreateUserService 명명도 나쁘지 않을 듯
    private val createUserPort: CreateUserPort
): CreateUserUseCase {
    @Transactional
    override fun createUser(
        createUserInput: CreateUserUseCase.CreateUserInput
    ): CreateUserUseCase.CreateUserOutput {
        val user = createUserPort.create(createUserInput.email, createUserInput.password, createUserInput.name)
        return CreateUserUseCase.fromDomainEntity(user)
    }
}