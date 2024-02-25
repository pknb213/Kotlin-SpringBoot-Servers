package com.example.repository

import com.example.entitys.user.UserRepository
import com.example.user.entity.User

interface UserInfraRepo: UserRepository {
    fun save(user: User)
//    override fun findByEmail(email: String): User? {
//        println("Email: ${email}")
//        return User.of(email, "1234", "yj")
//    }
}