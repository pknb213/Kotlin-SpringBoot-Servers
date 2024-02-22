package com.example.user.entity

import com.example.common.CommonEntity
import jakarta.persistence.*

@Entity
@Table(name = "user")
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0L,
    @Column(name = "email") val email: String,
    @Column(name = "password") val password: String,
    @Column(name = "name") val name: String,
    @Column(name = "roleId") val roleId: Long
): CommonEntity() {
}