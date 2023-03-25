package com.example.springbootrestserver.global.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

@Component
class JwtTokenFilter(
    private val jwtService: JwtService
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {

        val request = exchange.request
        val response = exchange.response
        val paramMap = exchange.request.queryParams
        val path = exchange.request.path.value()
        println("\n>> Req: $request\n>>Res: $response\n>>Parm: $paramMap\n>> Path: $path")

        val body = exchange.request.body
        return DataBufferUtils.join(body)
            .flatMap { dataBuffer ->
                val byteBuffer = dataBuffer.asByteBuffer()
                val byteArray = ByteArray(byteBuffer.remaining())
                byteBuffer.get(byteArray)
                val bodyJson = String(byteArray, StandardCharsets.UTF_8)
                println("\n>> Body Json: $bodyJson")
                val objectMapper = ObjectMapper().registerModule(KotlinModule())
                val bodyObject = objectMapper.readValue(bodyJson) as Any
                println(
                    "\n>> ${bodyObject.javaClass}, ${
                        bodyObject == mapOf(
                            "userId" to "test",
                            "password" to "1234"
                        )
                    }"
                )

                // Todo: 위에 예시 처럼 UserService에서 id, passwrod 가져와서 비교하고 토큰 발급하고 이런 과정 줘야할 듯
                // 1. userService로 데이터 가져오기

                // 2. 비교

                // 2-1. DB에 등록된 user 일 때

                // 3. Token 발급

                // 4. Token exchange에 저장

                // 5. filter chain 종료

                // 2-2. DB에 등록되지 않은 유저일 때, filter chain 종료

                chain.filter(exchange)
            }

        val token = extractTokenFromHeader(exchange)
        println("Token: $token")
        if (token != null && jwtService.isValidToken(token)) {
            val authentication = jwtService.getAuthentication(token)
            println("Auth: $authentication")
            return chain.filter(exchange).contextWrite(
                ReactiveSecurityContextHolder.withAuthentication(authentication)
            )
        }
        return chain.filter(exchange).then(Mono.fromRunnable {
            // print postHandle message
            println("Interceptor: response status ${exchange.response.statusCode}")

        })
    }

    private fun extractTokenFromHeader(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        println("Auth Header: $authHeader")
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            println("Starts With Brarer ~${authHeader.substring(7)}")
            authHeader.substring(7)
        } else null
    }
}
