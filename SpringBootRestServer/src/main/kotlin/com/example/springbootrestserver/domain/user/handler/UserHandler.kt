package com.example.springbootrestserver.domain.user.handler

import com.example.springbootrestserver.domain.user.domain.toDto
import com.example.springbootrestserver.domain.user.dto.LoginDto
import com.example.springbootrestserver.domain.user.dto.UserDto
import com.example.springbootrestserver.domain.user.dto.toEntity
import com.example.springbootrestserver.domain.user.service.UserService
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Component
class UserHandler(
    private val userService: UserService
) {
    suspend fun ping(req: ServerRequest): ServerResponse {
        println("Pong")
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).buildAndAwait()
    }
    suspend fun login(req: ServerRequest): ServerResponse {
        val receivedUser = req.awaitBodyOrNull(LoginDto::class)
        return receivedUser?.let {
            println("User: $receivedUser")
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(
                    userService.login(it)
//                    LoginDto("미친씨팔", "깨씨발좆같은스프링")
//                    mapOf("미친씨팔" to "깨씨발좆같은스프링")
                )
        } ?: ServerResponse.badRequest().buildAndAwait()
    }
    suspend fun getAll(req: ServerRequest): ServerResponse {
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(
                userService.find().map { it.toDto() }
            )
    }

    suspend fun getOne(req: ServerRequest): ServerResponse {
        val id = Integer.parseInt(req.pathVariable("id"))
        val existingUser = userService.findById(id.toLong())
        return existingUser?.let {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(it.toDto())
        } ?: ServerResponse.notFound().buildAndAwait()
    }

    suspend fun postOne(req: ServerRequest): ServerResponse {
        val receivedUser = req.awaitBodyOrNull(UserDto::class)
        return receivedUser?.let {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(
                    userService.insert(it.toEntity()) //.toDto()
                )
        } ?: ServerResponse.badRequest().buildAndAwait()
    }

    suspend fun putOne(req: ServerRequest): ServerResponse {
        val id = Integer.parseInt(req.pathVariable("id"))
        val receivedUser = userService.update(id.toLong())
        return receivedUser?.let {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(it.toDto())
        } ?: ServerResponse.badRequest().buildAndAwait()
    }

    suspend fun deleteOne(req: ServerRequest): ServerResponse {
        val id = Integer.parseInt(req.pathVariable("id"))
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(
                userService.delete(id.toLong())
            )
    }
}