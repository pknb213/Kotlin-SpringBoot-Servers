package com.example.entitys.user

import com.example.entitys.common.CommonEntity
import jakarta.persistence.*

@Table(name = "users")
@Entity
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0L,
    @Column(name = "email") val email: String,
    @Column(name = "password") val password: String,
    @Column(name = "name") val name: String,
    @Column(name = "roleId") val roleId: Long
): CommonEntity() {
}