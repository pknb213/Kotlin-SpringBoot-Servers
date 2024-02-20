package com.example

fun String.isNotNumber(): Boolean = this.any { !it.isDigit() }
