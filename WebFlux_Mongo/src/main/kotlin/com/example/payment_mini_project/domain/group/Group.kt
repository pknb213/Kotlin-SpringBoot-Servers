package com.example.payment_mini_project.domain.group

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Group(
    @Id val groupId: Number = Math.random().hashCode(),
    val condition: String? = null,
    val description: String? = null
)