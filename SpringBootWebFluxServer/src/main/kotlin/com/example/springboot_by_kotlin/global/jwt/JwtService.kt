package com.example.springboot_by_kotlin.global.jwt

import com.example.springboot_by_kotlin.domain.user.domain.UserRole
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
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
        // Todo: Need to Env
        private const val AUTHORITIES_KEY = "authorities"
        private const val SUBJECT_KEY = "sub"
        private const val ID_KEY = "jti"
        private const val ISSUE_AT_KEY = "iat"
        private const val EXPIRATION_KEY = "exp"
    }

    fun generateToken(id: Long?, role: UserRole): String {
        println("Token Generate Start")
        val now = Date()
        val validity = Date(now.time + expirationTime)
        val keyBytes = secretKey.toByteArray(StandardCharsets.UTF_8)
//        val encodedKeyBytes = Base64.getEncoder().encode(keyBytes)

        return Jwts.builder()
            .setSubject("test auth name")
            .claim(AUTHORITIES_KEY, role)
            .setId(id.toString())
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey.toByteArray(StandardCharsets.UTF_8))
            .compact()
    }

    fun isValidToken(token: String): Boolean {
        return try {
            val getAuth = getAuthentication(token)
            val dateTime = Date((getAuth["Expiration"].toString().toLong()) * 1000)
            println("\nToken Info: $getAuth\nToken Create Date: $dateTime\nIs After: ${dateTime.after(Date())}")
            println("Date Compare: $dateTime, ${Date()}")
            dateTime.after(Date())
        } catch (e: JwtException) {
            println("Invalid Token - Jwt Exception: $e")
            false
        } catch (e: IllegalArgumentException) {
            println("Invalid Token - IllegalArg Exception: $e")
            false
        } catch (e: Exception) {
            println("Invalid Token - Is Validated Exception: $e")
            false
        }
    }

    fun getAuthentication(token: String): Map<String, Any> {
        return try {
            val claims = Jwts.parser()
                .setSigningKey(secretKey.toByteArray(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .body
            mapOf(
                "Id" to claims[ID_KEY].toString(),
                "Authorities" to claims[AUTHORITIES_KEY].toString().split(","),
                "Expiration" to claims[EXPIRATION_KEY].toString(),
                "Subject" to claims[SUBJECT_KEY].toString(),
                "IssuedAt" to claims[ISSUE_AT_KEY].toString()
            )
        } catch (e: MalformedJwtException) {
            println("Invalid JWT Parser - Jwt Exception: $e")
            mapOf("success" to false, "msg" to e)
        } catch (e: Exception) {
            println("Invalid Token - Is invalidated Exception: $e")
            mapOf("success" to false, "msg" to e)
        }
    }

//    fun getAuthentication(token: String): Authentication {
//        val claims = Jwts.parser()
//            .setSigningKey(secretKey)
//            .parseClaimsJws(token)
//            .body
//        val authorities = claims[AUTHORITIES_KEY].toString()
//            .split(",")
//            .map { SimpleGrantedAuthority(it) }
//        val userDetails = userDetailsService.loadUserByUsername(claims.subject)
//        println("Claims: ${claims}\nAuthorities: ${authorities}\nUserDetails: ${userDetails}")
//        return UsernamePasswordAuthenticationToken(userDetails, "", authorities)
//    }
}