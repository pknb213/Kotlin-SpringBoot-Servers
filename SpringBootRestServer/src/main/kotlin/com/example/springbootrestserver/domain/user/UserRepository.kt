package com.example.springbootrestserver.domain.user

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository:CoroutineCrudRepository<User, Long> {
    override fun findAll(): Flow<User>

}