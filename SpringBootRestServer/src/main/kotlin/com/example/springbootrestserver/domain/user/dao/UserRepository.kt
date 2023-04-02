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

//    @Query("""
//        select * from user where user.email=(:email) and user.password=(:password)
//    """)
//    fun findByEmailAndPassword(email: String, password: String): Flow<User>
    @Query("""
        SELECT * FROM user
    """)
    fun findByEmailAndPassword(): Flow<User>

    @Query("""
        select * from user
    """)
    fun findByField(vararg field: String): User?
}