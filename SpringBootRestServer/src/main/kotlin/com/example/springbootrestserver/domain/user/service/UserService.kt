package com.example.springbootrestserver.domain.user.service

import com.example.springbootrestserver.domain.user.domain.User
import com.example.springbootrestserver.domain.user.dao.UserRepository
import com.example.springbootrestserver.domain.user.domain.UserRole
import com.example.springbootrestserver.domain.user.domain.toDto
import com.example.springbootrestserver.domain.user.dto.LoginDto
import com.example.springbootrestserver.domain.user.dto.SignUpDto
import com.example.springbootrestserver.domain.user.dto.UserDto
import com.example.springbootrestserver.domain.user.dto.toEntity
import com.example.springbootrestserver.global.common.StatusResponse
import com.example.springbootrestserver.global.jwt.JwtService
import io.jsonwebtoken.Jwts
import kotlinx.coroutines.flow.*
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import kotlin.math.log

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {
    suspend fun find(): Flow<User> = userRepository.findAll()
    suspend fun findById(id: Long): UserDto? = userRepository.findById(id)?.toDto()
    suspend fun insert(user: User): UserDto = userRepository.save(user).toDto()
    suspend fun update(id: Long): User? {
        return findById(id)?.let {
            userRepository.save(it.toEntity())
        }
    }
    suspend fun delete(id: Long): Unit = userRepository.deleteById(id)

    suspend fun login(loginDto: LoginDto): Map<String, Any> { // UserDto?
        // Todo: 여기에 이제 유저인지 확인하고 일치하면 JWT토큰을 발급하는 로직으로 변경. ex) { success: true, token: "ASdasdsad"}
        // Todo: 그리고 서비스에 대한 Response도 인터페이스를 하나로 일치하는게 좋을 것 같다. 위처럼 {msg: "xxx", success: true, data: []}
        // TOdo: 그러면 Handler도 현재 결과가 없을 때 NotFound 같이 하는 부분을 변경하는게 맞다
        println("\nLogin API\n  DTO: ${loginDto}")
        var token = ""
        val user = userRepository.findByEmailAndPassword(loginDto.email, loginDto.password)
        if (user === null) return mapOf("success" to false, "error" to "Invalid Id or Password")
        if (loginDto.token == null || !jwtService.isValidToken(loginDto.token.toString())) { // 토큰 없거나 유효 하지 않을 떄
            token = jwtService.generateToken(user.id, user.role)
            println("Created Token: ${jwtService.getAuthentication(token)}")
        } else token = loginDto.token.toString()
        return mapOf(
            "success" to true,
            "msg" to "${loginDto.email} login success.",
            "data" to listOf(mapOf("token" to token))
        )
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