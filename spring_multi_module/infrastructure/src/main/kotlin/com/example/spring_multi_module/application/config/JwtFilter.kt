package com.example.spring_multi_module.application.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.filter.GenericFilterBean

class JwtFilter: GenericFilterBean() {
    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain
    ) {
        val httpServletRequest = servletRequest as HttpServletRequest
        val jwt = 1
        val requestURL = httpServletRequest.requestURL
        println("Request URL: $requestURL")
        filterChain.doFilter(servletRequest, servletResponse)
    }
}