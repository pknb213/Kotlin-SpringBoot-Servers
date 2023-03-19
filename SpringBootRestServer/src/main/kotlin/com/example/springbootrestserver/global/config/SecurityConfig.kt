package com.example.springbootrestserver.global.config
//import JwtAuthenticationConverter
//import JwtAuthenticationManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter

//@Configuration
//@EnableWebFluxSecurity
//class SecurityConfig {
//    /**
//     * @Override
//     * @Bean
//     * public AuthenticationManager authenticationManagerBean() throws Exception {
//     *     return super.authenticationManagerBean();
//     * }
//     */
////    @Override
////    @Bean
////    fun authenticationManagerBean(): AuthenticationManager {
////        return super.authenticationManagerBean()
////    }
//    @Bean
//    fun passwordEncoder() = BCryptPasswordEncoder()
//
//    @Bean
//    fun securityWebFilterChain(
//        http: ServerHttpSecurity,
//        authManger: JwtAuthenticationManager,
//        converter: JwtAuthenticationConverter
//    ): SecurityWebFilterChain {
//        val filter = AuthenticationWebFilter(authManger)
//        filter.setServerAuthenticationConverter(converter)
//        println("security web filter chain start (in config directory)")
//        return http
//            .csrf().disable()
//            .formLogin().disable()
//            .httpBasic().disable()
//            .csrf().disable()
//            .logout().disable()
//            .authorizeExchange()
//            .pathMatchers("/**").permitAll()
//            .and()
//            .addFilterAfter(filter, SecurityWebFiltersOrder.AUTHENTICATION)
//            .authorizeExchange().anyExchange().authenticated().and()
//            .build()
//    }
//}

//addFilterAfter(filter, SecurityWebFiltersOrder.AUTHENTICATION) 이 동작이 필요