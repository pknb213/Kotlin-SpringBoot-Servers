package com.example.spring_multi_module.core.ports.`in`

import com.example.spring_multi_module.domain.entitys.user.User


interface CreateUserUseCase {
    /**
     * Command usecase (비지니스 상태를 바꾸는 유스케이스)인 경우 접미사 "UseCase"를 사용한다.
     *
     * applictation.port.in 과 같은 패키지 경로도 그대로 사용하기로 한다.
     * 프로젝트끼리 동일한 패키지 구조를 가진다면 구성원 간에 코드를 이해하는 속도가 크게 증가한다.
     */
    fun createUser(createUserInput: CreateUserInput): CreateUserOutput

    /**
     * web -> usecase로 요청 데이터를 넘길 때에는 DTO로서 command 객체를 사용하여야 한다.
     */
    data class CreateUserInput(
        val email: String,
        val password: String,
        val name: String
    ) {
        init {
            require(email.isNotEmpty()) { "email should not be empty" }
            require(password.isNotEmpty()) { "password should not be empty" }
            require(name.isNotEmpty()) { "name should not be empty" }
        }
    }

    /**
     * usecase --> web으로의 출력 모델도 usecase 별로 각자 정의한다. 가능한 최소한의 데이터를 노출하도록 설계하여야 한다.
     */
    data class CreateUserOutput(
        val ok: Boolean,
        val error: Error?,
        val data: UserData
    ) {
        data class UserData(
            val id: Long,
            val email: String,
            val name: String
        )
    }

    companion object {
//        val validateCommand
        fun fromDomain(user: User): CreateUserOutput {
            return CreateUserOutput(
                ok = true,
                error = null,
                data = CreateUserOutput.UserData(
                    id = user.id,
                    email = user.email,
                    name = user.name
                )
            )
        }
    }
}