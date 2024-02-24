package com.example.repository

import com.example.user.entity.User

interface UserRepository {
    fun save(user: User)
    fun findByEmail(email: String): User?
}