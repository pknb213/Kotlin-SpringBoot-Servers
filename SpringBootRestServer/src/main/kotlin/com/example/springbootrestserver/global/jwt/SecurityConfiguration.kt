package com.example.springbootrestserver.global.jwt

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

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration (
    private val jwtService: JwtService
) {
    @Bean
    fun jwtAuthenticationFilter(): JwtTokenFilter {
        return JwtTokenFilter(jwtService)
    }
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
//        return http
//            .csrf().disable()
//            .formLogin().disable()
//            .httpBasic().disable()
//            .authorizeExchange()
//            .pathMatchers("/login").permitAll()
//            .pathMatchers(HttpMethod.POST, "/login").permitAll()
//            .pathMatchers("/ping").permitAll() // /ping API에 대해 인증을 거치지 않도록 설정
//            .anyExchange().authenticated()
//            .and()
//            .build()

        return http
            .csrf().disable()
            .authorizeExchange()
            .pathMatchers("/ping").permitAll()
            .pathMatchers("/login").permitAll()
            .anyExchange().authenticated()
            .and()
            .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
//        return http
//            .httpBasic().disable()
//            .csrf().disable()
//            .authorizeExchange()
//            .anyExchange().permitAll()
//            .and()
//            .addFilterAt(JwtTokenFilter(jwtService), SecurityWebFiltersOrder.AUTHENTICATION)
//            .build()
    }


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun userDetailsService(): ReactiveUserDetailsService {
        val user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build()

        return MapReactiveUserDetailsService(user)
    }
}
