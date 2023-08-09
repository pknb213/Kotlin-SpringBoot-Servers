package com.example.springbootrestserver.domain.travel

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table
data class Travel(
    @Id var id: Long? = null,
    val name: String,
    val city_id: Long,
    val start_date: LocalDateTime,
    val end_date: LocalDateTime,
    val created_date: LocalDateTime = LocalDateTime.now(),
    val updated_date: LocalDateTime = LocalDateTime.now()
)

fun Travel.toDto(): TravelDto = TravelDto(
    name = name,
    city_id = city_id,
    start_date = start_date,
    end_date = end_date
)
