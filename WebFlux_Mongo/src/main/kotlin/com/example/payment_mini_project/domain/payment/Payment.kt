package com.example.payment_mini_project.domain.payment

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Payment(
    @Id val paymentId: Number = Math.random().hashCode(),
    @Indexed val accountId: Number,
    val amount: Number,
    val methodType: String,
    val itemCategory: String,
    val region: String
)
