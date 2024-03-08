package com.example.spring_multi_module.ports.out

import com.example.spring_multi_module.domain.entitys.com.example.spring_multi_module.entitys.user.User

interface GetAllUserPort {
    fun getAll(): List<User>
}