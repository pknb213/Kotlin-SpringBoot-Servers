package com.example.springbootrestserver.domain.user.dao

import com.example.springbootrestserver.domain.user.domain.User
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository:CoroutineCrudRepository<User, Long> {
    override fun findAll(): Flow<User>
    override suspend fun findById(id: Long): User?
    override suspend fun existsById(id: Long): Boolean
    override suspend fun <S : User> save(entity: S): User
    override suspend fun deleteById(id: Long)

    @Query("""
        
    """)
    fun findByEmailAndPassword(email: String, password: String): User?

    @Query("""
        
    """)
    fun findByField(vararg field: String): User?
}