import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import java.util.*

@Service
class JwtService(
    private val secretKey: String,
    private val expirationTime: Long,
    private val userDetailsService: UserDetailsService
) {
    companion object {
        private const val AUTHORITIES_KEY = "auth"
    }

    fun generateToken(authentication: Authentication): String {
        val authorities = authentication.authorities
            .map { it.authority }
            .joinToString(separator = ",")
        val now = Date()
        val validity = Date(now.time + expirationTime)

        return Jwts.builder()
            .setSubject(authentication.name)
            .claim(AUTHORITIES_KEY, authorities)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact()
    }

    fun isValidToken(token: String?): Boolean {
        return try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun getAuthentication(token: String): Authentication {
        val claims = Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .body
        val authorities = claims[AUTHORITIES_KEY].toString()
            .split(",")
            .map { SimpleGrantedAuthority(it) }
        val userDetails = userDetailsService.loadUserByUsername(claims.subject)
        return UsernamePasswordAuthenticationToken(userDetails, "", authorities)
    }
}
