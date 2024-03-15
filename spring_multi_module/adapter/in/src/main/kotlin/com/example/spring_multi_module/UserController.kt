package com.example.spring_multi_module

import com.example.spring_multi_module.ports.`in`.CreateUserUseCase
import com.example.spring_multi_module.ports.`in`.GetAllUserUseCase
import com.example.spring_multi_module.ports.`in`.DeleteUserUseCase
import com.example.spring_multi_module.ports.`in`.PutUserUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController(
    private val createUserUseCase: CreateUserUseCase,
    private val getAllUserUseCase: GetAllUserUseCase,
    private val putUserUseCase: PutUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase
) {
    @PostMapping
    fun createUser(
        @RequestBody createUserInput: CreateUserUseCase.CreateUserInput
    ): CreateUserUseCase.CreateUserOutput {
        println("Create User API")
        val (email, password, name) = createUserInput
        return createUserUseCase.createUser(
            CreateUserUseCase.CreateUserInput(
                email = email,
                password = password,
                name = name
            )
        )
    }

    @GetMapping
    fun getAllUser(): GetAllUserUseCase.GetAllUserOutput {
        println("Get All User API")
        return getAllUserUseCase.getAllUser()
    }

    @PutMapping
    fun putUserPassword(
        @RequestBody putUserPasswordInput: PutUserUseCase.PutUserInput
    ): PutUserUseCase.PutUserOutput {
        println("Put User Password API")
        val (email, password, name, roleId) = putUserPasswordInput
        return putUserUseCase.putUser(
            PutUserUseCase.PutUserInput(
                email = email,
                password = password,
                name = name,
                roleId =  roleId
            )
        )
    }

    @DeleteMapping("/{id}")
    fun deleteUser(
        @PathVariable id: Long
    ): DeleteUserUseCase.DeleteUserOutput {
        println("Delete User API")
        return deleteUserUseCase.delete(id)
    }
}