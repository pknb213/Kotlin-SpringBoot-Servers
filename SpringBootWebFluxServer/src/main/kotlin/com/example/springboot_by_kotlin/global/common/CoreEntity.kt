package com.example.springboot_by_kotlin.global.common

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

open class CoreEntity {
    @Id var id: Long? = null
    var created_date: LocalDateTime = LocalDateTime.now()
    var updated_date: LocalDateTime = LocalDateTime.now()
}