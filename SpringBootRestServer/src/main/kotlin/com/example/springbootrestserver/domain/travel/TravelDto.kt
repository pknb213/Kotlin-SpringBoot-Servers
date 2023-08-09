package com.example.springbootrestserver.domain.travel

import java.time.LocalDateTime

data class TravelDto (
    val name: String,
    val city_id: Long,
    val start_date: LocalDateTime,
    val end_date: LocalDateTime
)

fun TravelDto.toEntity(): Travel = Travel(
    name = name,
    city_id = city_id,
    start_date = start_date,
    end_date = end_date
)