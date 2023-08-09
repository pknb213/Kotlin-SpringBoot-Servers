package com.example.springboot_by_kotlin.domain.city

import java.time.LocalDateTime

data class CityDto (
    val name: String
)

fun CityDto.toEntity(): City = City(
    name = name
)