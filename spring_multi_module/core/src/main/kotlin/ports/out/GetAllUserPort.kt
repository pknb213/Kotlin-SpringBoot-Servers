package com.example.spring_multi_module.core.ports.out

import com.example.spring_multi_module.domain.entitys.user.User

interface GetAllUserPort {
    fun getAll(): List<User>
}