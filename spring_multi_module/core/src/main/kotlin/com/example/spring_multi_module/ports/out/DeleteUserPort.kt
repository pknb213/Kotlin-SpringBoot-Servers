package com.example.spring_multi_module.ports.out

interface DeleteUserPort {
    fun deleteUser(id: Long): Boolean
}