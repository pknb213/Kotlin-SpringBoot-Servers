package com.example.springbootrestserver.domain.city

data class CityDto (
    val name: String
)

fun CityDto.toEntity(): City = City(
    name = name
)