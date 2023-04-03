package com.example.springboot_by_kotlin.domain.statistic

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface StatisticRepository: CoroutineCrudRepository<Statistic, Long> {
        override suspend fun <S : Statistic> save(entity: S): Statistic
}