package com.example.springbootrestserver.global.jwt

import com.example.springbootrestserver.domain.user.dto.LoginDto
import com.example.springbootrestserver.domain.user.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

@Component
class JwtTokenFilter(
    private val jwtService: JwtService,
    private val userService: UserService
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val authorizationHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val token = extractToken(authorizationHeader)
        if (token != null && jwtService.isValidToken(token)) {
            println("Token Valid를 확인하겠슴미다. => $token")
            val username = jwtService.getAuthentication(token)
            println(username)
//            val user = userService.findByUsername(username)
//            if (user != null) {
//                val authentication = UsernamePasswordAuthenticationToken(user, null, user.authorities)
//                SecurityContextHolder.getContext().authentication = authentication
//            }
        }
        return chain.filter(exchange)
    }
    private fun extractToken(header: String?): String? {
        if (header == null || !header.startsWith("Bearer ")) {
            return null
        }
        return header.substring(7)
    }
    private fun extractTokenFromHeader(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        println("Auth Header: $authHeader")
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            println("Starts With Brarer ~${authHeader.substring(7)}")
            authHeader.substring(7)
        } else null
    }
}
