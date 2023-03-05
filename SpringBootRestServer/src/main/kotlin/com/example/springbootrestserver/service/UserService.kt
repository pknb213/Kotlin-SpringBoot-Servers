package com.example.springbootrestserver.service

import com.example.springbootrestserver.domain.user.User
import com.example.springbootrestserver.domain.user.UserRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun showAll(): Flow<User> = userRepository.findAll()
}