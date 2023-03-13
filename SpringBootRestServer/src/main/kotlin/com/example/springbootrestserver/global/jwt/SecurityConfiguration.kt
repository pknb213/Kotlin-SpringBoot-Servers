import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@ComponentScan("com.example.springbootrestserver.global.jwt")
@EnableWebFluxSecurity
class SecurityConfiguration (
    private val jwtService: JwtService

) {
    @Bean
    fun jwtAuthenticationFilter(): JwtTokenFilter {
        return JwtTokenFilter(jwtService)
    }
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
//        return http.authorizeExchange()
//            .pathMatchers("/api/user").authenticated()
//            .anyExchange().permitAll()
//            .and().addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
//            .csrf().disable()
//            .build()
        return http
            .csrf().disable()
            .authorizeExchange()
            .anyExchange().permitAll()
            .and()
            .addFilterAt(JwtTokenFilter(jwtService), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun userDetailsService(): ReactiveUserDetailsService {
        val user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build()

        return MapReactiveUserDetailsService(user)
    }
}
