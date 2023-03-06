package com.example.springbootrestserver.global.common

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

open class CoreEntity {
    @Id var id: Long? = null
    val created_date: LocalDateTime = LocalDateTime.now()
    val updated_date: LocalDateTime = LocalDateTime.now()
}