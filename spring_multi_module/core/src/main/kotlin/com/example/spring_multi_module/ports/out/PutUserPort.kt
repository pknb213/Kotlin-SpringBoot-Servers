package com.example.spring_multi_module.ports.out

import com.example.spring_multi_module.entitys.user.UserRole

interface PutUserPort {
    fun put(
        email: String,
        password: String,
        name: String,
        roleId: UserRole
    ): Boolean
}