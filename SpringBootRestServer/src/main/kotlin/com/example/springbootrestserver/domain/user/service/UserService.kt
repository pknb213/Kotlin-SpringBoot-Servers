package com.example.springbootrestserver.domain.user.service

import com.example.springbootrestserver.domain.user.domain.User
import com.example.springbootrestserver.domain.user.dao.UserRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun showAll(): Flow<User> = userRepository.findAll()
    
}