package com.example.springbootrestserver.global.jwt

import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtTokenFilter(
    private val jwtService: JwtService
): WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractTokenFromHeader(exchange)
        println("Token: $token")
        if (token != null && jwtService.isValidToken(token)) {
            val authentication = jwtService.getAuthentication(token)
            println("Auth: $authentication")
            return chain.filter(exchange).contextWrite(
                ReactiveSecurityContextHolder.withAuthentication(authentication))
        }
        return chain.filter(exchange).then<Void?>(Mono.fromRunnable {
            // print postHandle message
            println("Interceptor: response status ${exchange.response.statusCode}")

        })
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
