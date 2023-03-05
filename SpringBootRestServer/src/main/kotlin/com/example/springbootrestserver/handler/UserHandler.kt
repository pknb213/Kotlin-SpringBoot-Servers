package com.example.springbootrestserver.handler

import com.example.springbootrestserver.domain.user.User
import com.example.springbootrestserver.domain.user.UserRepository
import com.example.springbootrestserver.service.UserService
import kotlinx.coroutines.flow.flow
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait

@Component
class UserHandler (
    private val userService: UserService
){
    suspend fun getAll(req: ServerRequest): ServerResponse {
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(
                userService.showAll()
            )
    }
}