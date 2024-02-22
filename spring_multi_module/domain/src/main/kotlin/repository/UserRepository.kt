package com.example.repository

import com.example.entity.User

interface UserRepository {
    fun save(user: User)
    fun findByEmail(email: String): User?
}