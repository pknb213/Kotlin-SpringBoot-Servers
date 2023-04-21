package com.example.springbootrestserver.domain.user.service

import com.example.springbootrestserver.domain.user.domain.User
import com.example.springbootrestserver.domain.user.dao.UserRepository
import com.example.springbootrestserver.domain.user.domain.UserRole
import com.example.springbootrestserver.domain.user.domain.toDto
import com.example.springbootrestserver.domain.user.dto.LoginDto
import com.example.springbootrestserver.domain.user.dto.SignUpDto
import com.example.springbootrestserver.global.common.StatusResponse
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(
    private val userRepository: UserRepository
) {
    suspend fun find(): Flow<User> = userRepository.findAll()
    suspend fun findById(id: Long): User? = userRepository.findById(id)
    suspend fun insert(user: User): User = userRepository.save(user)
    suspend fun update(id: Long): User? {
        return findById(id)?.let {
            userRepository.save(it)
        }
    }
    suspend fun delete(id: Long): Unit = userRepository.deleteById(id)
    suspend fun findByEmailAndPassword(email: String, password: String): Flow<User> {
        println("Email: $email, Pwd: $password")
//        val user = userRepository.findByEmailAndPassword(email, password)
        val user = userRepository.findByEmailAndPassword()
        println(">>> $user")
        return user
//        return User(email="test", password = "1234", name = "test", role = UserRole.Client, verified = true)
    }

    suspend fun login(loginDto: LoginDto): User { //  Map<String, Any?>
//         TOdo: 씨팔 이거 걍 통합해 씨발
//        val user = userRepository.findByEmailAndPassword(loginDto.email, loginDto.password)
//        println(user)
//        return userRepository.findByEmailAndPassword(loginDto.email, loginDto.password)
//            .map { it.toDto() }
//        return mapOf("개씨발 꼴받아 뒤지곘네" to "나가 다 뒤져라")
//        return userRepository.save(User("asdsad", "Asd", "asd", UserRole.Client, true))
//        return userRepository.findAll().map {
//            println(">> $it")
//            LoginDto("미친씨팔", "깨씨발좆같은스프링")
//        }.first()
//        return userRepository.findByEmailAndPassword().map {
//            print(">> $it")
//            LoginDto("미친씨팔", "깨씨발좆같은스프링")
//        }.first()
//        return findByEmailAndPassword(loginDto.email, loginDto.password)?.let {
//            // 유저 있음 로직 (validate)
//            println("\n>> DB Res: $it")
//            StatusResponse.status200Ok(it)
//        } ?: StatusResponse.status404NotFound()
    }
    suspend fun SignUp(signUpDto: SignUpDto): Map<String, Any?> {
        return userRepository.findByField()?.let {

            StatusResponse.status200Ok()
        } ?: StatusResponse.status404NotFound()
    }
    suspend fun editUserProfile(): Map<String, Any?> {
        return userRepository.findByField()?.let {

            StatusResponse.status200Ok()
        } ?: StatusResponse.status404NotFound()
    }
}