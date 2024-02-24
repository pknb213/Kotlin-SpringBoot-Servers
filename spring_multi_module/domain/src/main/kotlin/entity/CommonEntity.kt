package com.example.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@MappedSuperclass
@EntityListeners(value = [AuditingEntityListener::class])
abstract class CommonEntity {
    @Column(name = "create_at", nullable = false, updatable = false, columnDefinition = "DATE")
    var createdAt: ZonedDateTime = ZonedDateTime.now()
        protected set

    @Column(name = "updated_at", columnDefinition = "DATE")
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
        protected set
}