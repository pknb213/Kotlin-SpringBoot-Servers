package com.example.spring_multi_module

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface IUserRepository: JpaRepository<UserEntity, Long> {

}