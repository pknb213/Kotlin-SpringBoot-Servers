package com.example.springboot_by_kotlin.global.jwt

import com.example.springboot_by_kotlin.domain.user.service.UserService
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class JwtFilter (
    private val jwtService: JwtService,
    private val userService: UserService
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val authorizationHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val token = extractToken(authorizationHeader)
        if (token != null && jwtService.isValidToken(token)) {
            println("Valid Token. => $token")

//            val user = userService.findByUsername(username)
//            if (user != null) {
//                val authentication = UsernamePasswordAuthenticationToken(user, null, user.authorities)
//                SecurityContextHolder.getContext().authentication = authentication
//            }
            return chain.filter(exchange)
        }
        println("Invalid Token.")
        return chain.filter(exchange)
    }

    private fun extractToken(header: String?): String? {
        if (header == null || !header.startsWith("Bearer ")) {
            return null
        }
        return header.substring(7)
    }

//    private fun extractTokenFromHeader(exchange: ServerWebExchange): String? {
//        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
//        println("Auth Header: $authHeader")
//        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            println("Starts With Brarer ~${authHeader.substring(7)}")
//            authHeader.substring(7)
//        } else null
//    }
}