import com.example.springbootrestserver.global.jwt.JwtInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Configuration
@EnableWebFlux
class WebConfig(private val jwtInterceptor: JwtInterceptor) : WebFluxConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(jwtInterceptor)
    }

    @Bean
    fun webFilterChain(jwtInterceptor: JwtInterceptor): WebFilterChainFilter {
        return WebFilterChainFilter(jwtInterceptor)
    }
}

class WebFilterChainFilter(private val jwtInterceptor: JwtInterceptor) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono
