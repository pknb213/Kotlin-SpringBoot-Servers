package com.example.springboot_by_kotlin.domain.city

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

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