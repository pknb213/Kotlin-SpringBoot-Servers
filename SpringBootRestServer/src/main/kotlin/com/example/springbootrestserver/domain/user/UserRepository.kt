package com.example.springbootrestserver.domain.user

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository:CoroutineCrudRepository<User, Long> {
    override fun findAll(): Flow<User>
    override suspend fun findById(id: Long): User?
    override suspend fun existsById(id: Long): Boolean
    override suspend fun <S : User> save(entity: S): User
    override suspend fun deleteById(id: Long)
}