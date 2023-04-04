package com.example.springboot_by_kotlin.global.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class JwtService(
    @Value("\${jwt.secret-key}")
    private val secretKey: String,
    @Value("\${jwt.expirationTime}")
    private val expirationTime: Long,
//    private val userDetailsService: UserDetailsService
) {
    companion object {
        private const val AUTHORITIES_KEY = "auth"
    }
//    fun generateToken(authentication: Authentication): String {
//        println("Token Generate Start")
//        val authorities = authentication.authorities
//            .map { it.authority }
//            .joinToString(separator = ",")
//        val now = Date()
//        val validity = Date(now.time + expirationTime)
//
//        return Jwts.builder()
//            .setSubject(authentication.name)
//            .claim(AUTHORITIES_KEY, authorities)
//            .setIssuedAt(now)
//            .setExpiration(validity)
//            .signWith(SignatureAlgorithm.HS256, secretKey)
//            .compact()
//    }
    fun generateToken(): String {
        println("Token Generate Start")
//        val authorities = authentication.authorities
//            .map { it.authority }
//            .joinToString(separator = ",")
        val now = Date()
        val validity = Date(now.time + expirationTime)

        return Jwts.builder()
            .setSubject("test auth name")
            .claim(AUTHORITIES_KEY, "JWT,Test")
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact()
    }

    fun isValidToken(token: String?): Boolean {
        println("Is Valid Token?")
        return try {
            val encodedKey = Base64.getEncoder().encodeToString(secretKey.toByteArray())
            Jwts.parser().setSigningKey(encodedKey).parseClaimsJwt(token)
            true
        } catch (e: JwtException) {
            println("Invalid Token: \nJwt Exception: ${e}")
            false
        } catch (e: IllegalArgumentException) {
            println("Invalid Token: \nIllegalArg Exception: ${e}")
            false
        } catch (e: Exception) {
            println("Invalid Token: \nIs Valided Exception: ${e}")
            false
        }
    }

//    fun getAuthentication(token: String): Authentication {
//        println("GET Auth?")
//        val claims = Jwts.parser()
//            .setSigningKey(secretKey)
//            .parseClaimsJws(token)
//            .body
//        val authorities = claims[AUTHORITIES_KEY].toString()
//            .split(",")
//            .map { SimpleGrantedAuthority(it) }
//        val userDetails = userDetailsService.loadUserByUsername(claims.subject)
//        return UsernamePasswordAuthenticationToken(userDetails, "", authorities)
//    }
}