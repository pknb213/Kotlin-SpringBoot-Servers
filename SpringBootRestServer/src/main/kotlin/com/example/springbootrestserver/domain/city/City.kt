package com.example.springbootrestserver.domain.city

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table
data class City (
    @Id var id: Long? = null,
    val name: String,
    val created_date: LocalDateTime = LocalDateTime.now(),
    val updated_date: LocalDateTime = LocalDateTime.now()
)

fun City.toDto(): CityDto = CityDto(
    name = name,
)