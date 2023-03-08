package com.example.springbootrestserver.domain.user.service

import com.example.springbootrestserver.domain.user.domain.User
import com.example.springbootrestserver.domain.user.dao.UserRepository
import com.example.springbootrestserver.domain.user.dto.LoginDto
import com.example.springbootrestserver.global.common.StatusResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {
    suspend fun find(): Flow<User> = userRepository.findAll()
    suspend fun findById(id: Long): User? = userRepository.findById(id)
    suspend fun insert(user: User): User = userRepository.save(user)
    suspend fun update(id: Long): User? {
        return findById(id)?.let {
            userRepository.save(it)
        }
    }
    suspend fun delete(id: Long): Unit = userRepository.deleteById(id)
    suspend fun findByEmailAndPassword(email: String, password: String): User? =
        userRepository.findByEmailAndPassword(email, password)
    suspend fun login(loginDto: LoginDto): Map<String, Any?> {
        return findByEmailAndPassword(loginDto.email, loginDto.password)?.let {
            // 유저 있음 로직 (validate)
            StatusResponse.status200Ok(it)
        } ?: StatusResponse.status404NotFound()
    }
    suspend fun SignUp()
}