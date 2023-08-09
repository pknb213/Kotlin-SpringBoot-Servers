package com.example.springboot_by_kotlin.domain.travel

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime
import java.util.*

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
