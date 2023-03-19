//import com.daily.view.api.exception.BusinessException
//import com.daily.view.api.exception.ErrorCode
import com.example.springbootrestserver.domain.user.service.UserService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

//@Component
//class JwtAuthenticationManager(
//    private val jwtSupport: JwtSupport,
//    private val users: ReactiveUserDetailsService
//) : ReactiveAuthenticationManager {
//    override fun authenticate(authentication: Authentication?): Mono<Authentication> {
//        return Mono.justOrEmpty(authentication)
//            .filter { auth -> auth is BearerToken }
//            .cast(BearerToken::class.java)
//            .flatMap { jwt -> mono { validate(jwt) } }
//            .onErrorMap { error -> InvalidBearerToken(error.message) }
//    }
//
//    private suspend fun validate(token: BearerToken): Authentication {
//        val memberEmail = jwtSupport.getMemberEmail(token)
//        val user = users.findByUsername(memberEmail).awaitSingleOrNull()
//
//        if (jwtSupport.isValid(token, user)) {
//            return UsernamePasswordAuthenticationToken(user!!.username, user.password, user.authorities)
//        }
//
////        throw BusinessException(ErrorCode.INVALID_JWT_TOKEN, "유효하지 않은 jwt token 입니다.")
//        throw Error("\"유효하지 않은 jwt token 입니다.\"")
//    }
//}
//
//class InvalidBearerToken(message: String?): AuthenticationException(message)