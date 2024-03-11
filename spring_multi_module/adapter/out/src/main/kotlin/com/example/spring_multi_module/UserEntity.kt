package com.example.spring_multi_module

import com.example.spring_multi_module.entitys.user.User
import com.example.spring_multi_module.entitys.user.UserRole
import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserEntity (
    @GeneratedValue(strategy = GenerationType.IDENTITY) @Id val id: Long = 0L,
    @Column(name = "email") val email: String,
    @Column(name = "password") val password: String,
    @Column(name = "name") val name: String,
    @Column(name = "roleId") val roleId: UserRole
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

// 아래처럼 확장 함수 대신 object로 converter를 따로 만들 수 있다.
fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    password = password,
    name = name,
    roleId = roleId,
)