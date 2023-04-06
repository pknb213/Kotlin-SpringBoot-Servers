package com.example.springboot_by_kotlin.utils

import com.example.springboot_by_kotlin.domain.user.dao.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByField(username)
            ?: throw UsernameNotFoundException("User not found with username: $username")
        println("User Detail Service IMPL: $user")
        return User(
            user.name,
            user.password,
            listOf(SimpleGrantedAuthority(user.role.toString()))
        )
    }
}
