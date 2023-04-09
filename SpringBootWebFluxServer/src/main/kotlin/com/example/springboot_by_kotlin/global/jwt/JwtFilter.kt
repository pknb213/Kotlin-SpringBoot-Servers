package com.example.springboot_by_kotlin.global.jwt

import com.example.springboot_by_kotlin.domain.user.domain.User
import com.example.springboot_by_kotlin.domain.user.domain.UserRole
import com.example.springboot_by_kotlin.domain.user.service.UserService
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.*

class JwtFilter (
    private val jwtService: JwtService,
    private val userService: UserService,
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val authorizationHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val token = extractToken(authorizationHeader)
        println("<Filter> Extract Token: $token")
        if (token != null && jwtService.isValidToken(token)) {
            println("<Filter> Valid Token !!")
            val decodeToken = jwtService.getAuthentication(token)
            val id = decodeToken["Id"]
            val authorities = decodeToken["Authorities"] as List<*>
            val abc = authorities.map {
                SimpleGrantedAuthority(it.toString())
            }
                .toList()
            println("????? $abc, ${abc.javaClass}, ${abc.get(0)?.javaClass}")

//            val user = User(role = UserRole.ROLE_USER, email = "test", password = "1234", verified = true, name = "test")
            val authentication = UsernamePasswordAuthenticationToken(id, null, abc)//listOf(SimpleGrantedAuthority())
            println("?? $authentication")
            val securityContext = SecurityContextImpl(authentication)
            exchange.attributes.putIfAbsent("AUTHENTICATION", authentication)
            exchange.attributes.putIfAbsent("SECURITY_CONTEXT", securityContext)
            SecurityContextHolder.getContext().authentication = authentication
            val securityContextRepository = WebSessionServerSecurityContextRepository()

            // exchange에 security context를 저장
            return securityContextRepository.save(exchange, securityContext).then(
                chain.filter(exchange)
            )
        }
        else println("<Filter> Invalid Token")
        return chain.filter(exchange)
//        return exchange.response.setComplete()
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