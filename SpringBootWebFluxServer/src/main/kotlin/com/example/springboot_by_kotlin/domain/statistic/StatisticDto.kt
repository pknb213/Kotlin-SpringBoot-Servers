package com.example.springboot_by_kotlin.domain.statistic

import java.time.LocalDateTime


class StatisticDto (
    val city_id: Long
)

fun StatisticDto.toEntity(): Statistic = Statistic(
    city_id = city_id,
)
