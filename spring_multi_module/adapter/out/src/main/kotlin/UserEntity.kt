package com.example.spring_multi_module.adapter.out

import com.example.spring_multi_module.domain.entitys.user.User
import com.example.spring_multi_module.domain.entitys.user.UserRole
import jakarta.persistence.*
import java.time.ZonedDateTime

@Entity
@Table(name = "users")
class UserEntity (
    @GeneratedValue(strategy = GenerationType.IDENTITY) @Id val id: Long = 0L,
    @Column(nullable = false) val email: String,
    @Column(nullable = false) val password: String,
    @Column(nullable = false) val name: String,
    @Column(nullable = false) val roleId: UserRole
) {
    companion object {
        fun fromDomainEntity(user: User) = UserEntity(
            email = user.email,
            password = user.password,
            name = user.name,
            roleId = user.roleId,
        )
    }
}

fun UserEntity.toDomainEntity() = User(
    id = id,
    email = email,
    password = password,
    name = name,
    roleId = roleId,
)