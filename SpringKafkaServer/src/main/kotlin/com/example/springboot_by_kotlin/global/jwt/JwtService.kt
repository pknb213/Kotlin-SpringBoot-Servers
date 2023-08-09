package com.example.springboot_by_kotlin.global.jwt

import com.example.springboot_by_kotlin.domain.user.domain.UserRole
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SignatureException
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
        // Todo: Need to Env
        private const val AUTHORITIES_KEY = "authorities"
        private const val SUBJECT_KEY = "sub"
        private const val ID_KEY = "jti"
        private const val ISSUE_AT_KEY = "iat"
        private const val EXPIRATION_KEY = "exp"
    }

    fun generateToken(id: Long?, role: UserRole): String {
        val now = Date()
        val validity = Date(now.time + expirationTime)

        return Jwts.builder()
            .setSubject("test auth name")
            .claim(AUTHORITIES_KEY, role)
            .setId(id.toString())
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey.toByteArray())
            .compact()
    }

    fun isValidToken(token: String): Boolean {
        return try {
            val decodeToken = getAuthentication(token)
            val dateTime = Date((decodeToken["Expiration"].toString().toLong()) * 1000)
            dateTime.after(Date())
        } catch (e: SignatureException) {
            println("Invalid Token - Signature Exception: $e")
            false
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
                .setSigningKey(secretKey.toByteArray())
                .parseClaimsJws(token)
            mapOf(
                "Id" to claims.body[ID_KEY].toString(),
                "Authorities" to claims.body[AUTHORITIES_KEY].toString().split(","),
                "Expiration" to claims.body[EXPIRATION_KEY].toString(),
                "Subject" to claims.body[SUBJECT_KEY].toString(),
                "IssuedAt" to claims.body[ISSUE_AT_KEY].toString(),
                "Signature" to claims.signature
            )
        } catch (e: MalformedJwtException) {
            println("Invalid JWT Parser - Jwt Exception: $e")
            mapOf("success" to false, "msg" to e)
        } catch (e: Exception) {
            println("Invalid Parser - Is invalidated Exception: $e")
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