package com.example.spring_multi_module.entitys.common

import java.time.ZonedDateTime

abstract class CommonDomain {
    val createdAt: ZonedDateTime? = null
    val updatedAt: ZonedDateTime? = null
}

//@MappedSuperclass
//@EntityListeners(value = [AuditingEntityListener::class])
//abstract class CommonEntity {
//    @Column(name = "create_at", nullable = false, updatable = false, columnDefinition = "DATE")
//    var createdAt: ZonedDateTime = ZonedDateTime.now()
//        protected set
//
//    @Column(name = "updated_at", columnDefinition = "DATE")
//    var updatedAt: ZonedDateTime = ZonedDateTime.now()
//        protected set
//}
