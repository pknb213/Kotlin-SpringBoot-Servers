package com.example.springbootrestserver.global.jwt


import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtInterceptor: WebFilter {
    private val secretKey = "mySecretKey" // JWT secret key

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        // Authorization header에서 JWT 추출
        val token = exchange.request.headers["Authorization"]?.firstOrNull()?.removePrefix("Bearer ")

        if (token != null && validateToken(token)) {
            // JWT가 유효한 경우
            return chain.filter(exchange)
        } else {
            // JWT가 유효하지 않은 경우
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }
    }

    // JWT 유효성 검사
    private fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}