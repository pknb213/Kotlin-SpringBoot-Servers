package com.example.springbootrestserver.domain.statistic


class StatisticDto (
    val city_id: Long
)

fun StatisticDto.toEntity(): Statistic = Statistic(
    city_id = city_id,
)
