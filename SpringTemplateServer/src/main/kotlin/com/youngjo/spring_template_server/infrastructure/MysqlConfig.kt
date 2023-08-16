package com.youngjo.spring_template_server.infrastructure
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.util.concurrent.TimeUnit

@Configuration
@Profile("prod")
@EnableR2dbcRepositories
class MysqlConfig {
    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()

        // 설정 코드 추가
        val retryPolicy = SimpleRetryPolicy()
        retryPolicy.maxAttempts = 3 // 최대 재시도 횟수
        retryTemplate.setRetryPolicy(retryPolicy)

        // 재시도 시간 간격 설정
        val backOffPolicy = FixedBackOffPolicy()
        backOffPolicy.backOffPeriod = TimeUnit.SECONDS.toMillis(5) // 5초 간격으로 재시도
        retryTemplate.setBackOffPolicy(backOffPolicy)

        return retryTemplate
    }
    @Bean
    fun connectionFactory(@Autowired retryTemplate: RetryTemplate): ConnectionFactory {
        val options = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.SSL, true)
            .option(ConnectionFactoryOptions.DRIVER, "pool")
            .option(ConnectionFactoryOptions.PROTOCOL, "mysql")
            .option(ConnectionFactoryOptions.HOST, "localhost")
            .option(ConnectionFactoryOptions.PORT, 3306)
            .option(ConnectionFactoryOptions.USER, "root")
            .option(ConnectionFactoryOptions.PASSWORD, "devpassword")
            .option(ConnectionFactoryOptions.DATABASE, "mydb")
            .build()
        return retryTemplate.execute<ConnectionFactory, RuntimeException> {
            ConnectionFactories.get(options)
        }
    }
    @Bean
    fun initializeTable(
        @Qualifier("connectionFactory")
        connectionFactory: ConnectionFactory
    ): ConnectionFactoryInitializer {
        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)
        initializer.setDatabasePopulator(ResourceDatabasePopulator(ClassPathResource("migrations/V1__init.sql")))
        return initializer
    }
}