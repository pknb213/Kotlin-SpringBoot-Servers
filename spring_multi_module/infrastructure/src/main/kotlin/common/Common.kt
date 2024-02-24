package com.example.common

import java.time.ZonedDateTime

abstract class Common {
    var createdAt: ZonedDateTime? = null
    var updatedAt: ZonedDateTime? = null
    var isDeleted: Boolean? = null
}