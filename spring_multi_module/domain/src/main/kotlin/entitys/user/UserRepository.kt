//package com.example.entitys.user
//
//import com.example.entitys.user.UserEntity
//import org.springframework.data.jpa.repository.JpaRepository
//
//interface UserRepository: JpaRepository<UserEntity, Long> {
//    fun save(user: UserEntity)
//    fun findByEmail(email: String): UserEntity?
//}