package com.example.springboot_by_kotlin.domain.travel

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TravelRepository: CoroutineCrudRepository<Travel, Long> {
    override fun findAll(): Flow<Travel>
    override suspend fun findById(id: Long): Travel?
    override suspend fun existsById(id: Long): Boolean
    override suspend fun <S : Travel> save(entity: S): Travel
    override suspend fun deleteById(id: Long)
}