package com.example.springbootrestserver.domain.statistic

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table
data class Statistic(
    @Id var id: Long? = null,
    val city_id: Long,
    val accessed_date: LocalDateTime = LocalDateTime.now()
)

fun Statistic.toDto(): StatisticDto = StatisticDto(
    city_id = city_id
)