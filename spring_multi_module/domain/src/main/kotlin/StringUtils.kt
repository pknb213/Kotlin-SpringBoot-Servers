package com.example.spring_multi_module.domain.entitys

fun String.isNotNumber(): Boolean = this.any { !it.isDigit() }
