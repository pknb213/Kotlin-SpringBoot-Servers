package com.example.springboot_by_kotlin.domain.user.service

import com.example.springboot_by_kotlin.domain.user.domain.User
import com.example.springboot_by_kotlin.domain.user.dao.UserRepository
import com.example.springboot_by_kotlin.domain.user.domain.UserRole
import com.example.springboot_by_kotlin.domain.user.domain.toDto
import com.example.springboot_by_kotlin.domain.user.dto.LoginDto
import com.example.springboot_by_kotlin.domain.user.dto.SignUpDto
import com.example.springboot_by_kotlin.domain.user.dto.UserDto
import com.example.springboot_by_kotlin.domain.user.dto.toEntity
import com.example.springboot_by_kotlin.global.common.StatusResponse
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(
    private val userRepository: UserRepository
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

    suspend fun login(loginDto: LoginDto): UserDto? {
        // Todo: 여기에 이제 유저인지 확인하고 일치하면 JWT토큰을 발급하는 로직으로 변경. ex) { success: true, token: "ASdasdsad"}
        // Todo: 그리고 서비스에 대한 Response도 인터페이스를 하나로 일치하는게 좋을 것 같다. 위처럼 {msg: "xxx", success: true, data: []}
        // TOdo: 그러면 Handler도 현재 결과가 없을 때 NotFound 같이 하는 부분을 변경하는게 맞다
        return userRepository.findByEmailAndPassword(loginDto.email, loginDto.password)?.toDto()
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
//        return User(email = "Adfs", password = "asd", name = "adsf", role = UserRole.Client, verified = true)
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