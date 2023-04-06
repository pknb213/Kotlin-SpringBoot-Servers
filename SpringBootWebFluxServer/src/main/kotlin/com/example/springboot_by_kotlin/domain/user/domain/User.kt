package com.example.springboot_by_kotlin.domain.user.domain

import com.example.springboot_by_kotlin.domain.user.dto.UserDto
import com.example.springboot_by_kotlin.global.common.CoreEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

enum class UserRole {
    ROLE_USER,
    ROLE_BASIC,
    ROLE_CLIENT,
    ROLE_TEST,
    ROLE_ADMIN
}

@Table
class User(
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val verified: Boolean
): CoreEntity()

fun User.toDto(): UserDto = UserDto(
    name = name,
    email = email,
    password = password,
    role = role,
    verified = verified
)

/**
 *  public Long getId() {
 *         return id;
 *     }
 *
 *     @Override
 *     public boolean equals(Object o) {
 *         if (this == o) return true;
 *         if (o == null) return false;
 *         BaseEntity that = (BaseEntity) o;
 *         return Objects.equals(id, that.id);
 *     }
 *
 *     @Override
 *     public int hashCode() {
 *         return Objects.hash(id);
 *     }
 */