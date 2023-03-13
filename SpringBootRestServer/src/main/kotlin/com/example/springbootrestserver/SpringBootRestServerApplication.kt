package com.example.springbootrestserver

import com.example.springbootrestserver.global.jwt.JwtInterceptor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SpringBootRestServerApplication {
    @Bean
    fun jwtInterceptor(): JwtInterceptor {
        return JwtInterceptor()
    }
}

fun main(args: Array<String>) {
    runApplication<SpringBootRestServerApplication>(*args)
}
