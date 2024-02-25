package com.example.repository

import com.example.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository: JpaRepository<UserEntity, Long> {
    fun save(user: UserEntity)
    fun findByEmail(email: String): UserEntity?
}