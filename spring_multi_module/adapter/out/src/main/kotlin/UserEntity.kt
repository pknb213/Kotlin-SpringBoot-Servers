package com.example.spring_multi_module.adapter.out

import com.example.spring_multi_module.domain.entitys.user.User
import com.example.spring_multi_module.domain.entitys.user.UserRole
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class UserEntity (
    @Id val id: Long = 0L,
    val email: String,
    val password: String,
    val name: String,
    val roleId: UserRole
) {
    companion object {
        fun fromDomainEntity(user: User) = UserEntity(
            id = user.id,
            email = user.email,
            password = user.password,
            name = user.name,
            roleId = user.roleId
        )
    }
}

fun UserEntity.toDomainEntity() = User(
    id = id,
    email = email,
    password = password,
    name = name,
    roleId = roleId
)