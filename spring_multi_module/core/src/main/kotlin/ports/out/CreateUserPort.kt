package com.example.spring_multi_module.core.ports.out

import com.example.spring_multi_module.domain.entitys.user.User

/**
 * 의존성 역전을 이용하여 application core가 영속성 계층에 의존하지 않도록 하는 것이 클린 아키텍처의 핵심이다.
 * Usecase 구현체가 영속성 구현체가 아니라 여기의 영속성 인터페이스에 의존하게 하고, 실제 구현은 영속성 계층에서 진행한다.
 * ISP 원칙에 따라 outgoing port 또한 기능별로 세분화한다.
 */
interface CreateUserPort {
    fun create(
        email: String,
        password: String,
        name: String
    ): User
}