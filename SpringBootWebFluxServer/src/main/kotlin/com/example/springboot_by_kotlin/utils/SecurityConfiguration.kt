package com.example.springboot_by_kotlin.utils

import com.example.springboot_by_kotlin.domain.user.service.UserService
import com.example.springboot_by_kotlin.global.jwt.JwtFilter
import com.example.springboot_by_kotlin.global.jwt.JwtService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration (
    private val jwtService: JwtService,
    private val userService: UserService
) {
    @Bean
    fun jwtAuthenticationFilter(): JwtFilter {
        return JwtFilter(jwtService, userService)
    }
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        println("> spring security filter chain start")
        return http
            .csrf().disable()
            .authorizeExchange()
            .pathMatchers("/v1/**").permitAll()
            .anyExchange().authenticated()
            .and()
//            .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

//    @Bean
//    fun userDetailsService(): ReactiveUserDetailsService {
//        val user = User.builder()
//            .username("user")
//            .password(passwordEncoder().encode("password"))
//            .roles("USER")
//            .build()
//
//        return MapReactiveUserDetailsService(user)
//    }
}