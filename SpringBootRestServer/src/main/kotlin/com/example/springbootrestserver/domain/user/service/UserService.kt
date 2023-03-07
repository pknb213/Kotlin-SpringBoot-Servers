package com.example.springbootrestserver.domain.user.service

import com.example.springbootrestserver.domain.user.domain.User
import com.example.springbootrestserver.domain.user.dao.UserRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

interface ServiceInterface {
    val success: Boolean
    val error: Boolean
    val data: Array<Any>
}
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
    suspend fun findByEmailAndPassword(email: String, password: String) =
        userRepository.findByEmailAndPassword(email, password)
    suspend fun login(email: String, password: String) {
        return findByEmailAndPassword(email, password)?.let {

        } ?: mapOf("asd", "asd")
    }
}