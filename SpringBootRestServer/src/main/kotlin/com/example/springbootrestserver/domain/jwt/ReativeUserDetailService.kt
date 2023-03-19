package com.example.springbootrestserver.domain.jwt

//import com.daily.view.api.entity.member.MemberRepository
//import com.daily.view.api.service.auth.UserDetailsImpl
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

//@Service
//class UserDetailsService(
//    private val memberRepository: MemberRepository
//) : ReactiveUserDetailsService {
//
//    override fun findByUsername(username: String?): Mono<UserDetails> {
//        if (username == null) return Mono.empty()
//        val member = memberRepository.findByEmail(username) ?: return Mono.empty()
//        return Mono.just(UserDetailsImpl(member))
//    }
//}

//private suspend fun validate(token: BearerToken): Authentication {
//    val memberEmail = jwtSupport.getMemberEmail(token)
//    val user = users.findByUsername(memberEmail).awaitSingleOrNull()
//
//    if (jwtSupport.isValid(token, user)) {
//        return UsernamePasswordAuthenticationToken(user!!.username, user.password, user.authorities)
//    }
//
//    throw BusinessException(ErrorCode.INVALID_JWT_TOKEN, "유효하지 않은 jwt token 입니다.")
//}