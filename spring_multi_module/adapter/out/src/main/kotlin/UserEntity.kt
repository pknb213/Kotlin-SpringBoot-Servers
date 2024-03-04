package com.example.spring_multi_module.adapter.out

import com.example.spring_multi_module.domain.entitys.common.CommonEntity
import com.example.spring_multi_module.domain.entitys.user.User
import com.example.spring_multi_module.domain.entitys.user.UserRole
import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserEntity (
    @GeneratedValue(strategy = GenerationType.IDENTITY) @Id val id: Long,
    val email: String,
    val password: String,
    val name: String,
    val roleId: UserRole
): CommonEntity() {
    companion object {
        fun fromDomain(user: User) = UserEntity(
            id = user.id,
            email = user.email,
            password = user.password,
            name = user.name,
            roleId = user.roleId,
        )
    }
}

fun UserEntity.toDomain() = User(
    email = email,
    password = password,
    name = name,
    roleId = roleId,
)