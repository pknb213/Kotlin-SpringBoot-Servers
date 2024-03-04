package com.example.spring_multi_module.adapter.out

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface IUserRepository: JpaRepository<UserEntity, Long> {

}