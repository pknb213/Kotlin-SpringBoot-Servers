package com.example.payment_mini_project.domain.account

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Account(
    @Id val accountId: Number = Math.random().hashCode(),
    val residence: String,
    val age: Number
)