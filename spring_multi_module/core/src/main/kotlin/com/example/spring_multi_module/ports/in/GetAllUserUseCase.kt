package com.example.spring_multi_module.ports.`in`

import com.example.spring_multi_module.domain.entitys.com.example.spring_multi_module.entitys.user.User
import com.example.spring_multi_module.domain.entitys.com.example.spring_multi_module.entitys.user.UserRole
import java.lang.Error

interface GetAllUserUseCase {
    fun getAllUser(): GetAllUserOutput

    data class GetAllUserOutput(
        val ok: Boolean,
        val error: Error?,
        val data: List<UserData>
    ) {
        data class UserData(
//            val id: Long,
            val email: String,
            val name: String,
            val roleId: UserRole
        )
    }

    companion object {
        fun fromDomain(users: List<User>): GetAllUserOutput {
            val usersData = users.map {
                GetAllUserOutput.UserData(
//                    id = it.id,
                    email = it.email,
                    name = it.name,
                    roleId = it.roleId
                )
            }
            return GetAllUserOutput(
                ok = true,
                error = null,
                data = usersData
            )
        }
    }
}