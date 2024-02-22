package com.example

import com.example.entity.User
import com.example.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = false)
class UserService(
    private val userRepository: UserRepository
) {
    fun createUser() {
        val testMail = "test@naveff.com"
        val testPassword = "1234"
        val testName = "yj"
        userRepository.findByEmail(testMail)?.let {
            throw Error("중복된 계정입니다.")
        }
        val user = User.of(testMail, testPassword, testName)
        userRepository.save(user)
    }
}