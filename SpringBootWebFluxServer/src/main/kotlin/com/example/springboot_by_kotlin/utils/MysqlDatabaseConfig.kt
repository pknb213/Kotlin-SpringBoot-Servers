package com.example.springboot_by_kotlin.utils


import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.pool.PoolingConnectionFactoryProvider
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ValidationDepth
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.dialect.PostgresDialect
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.ReactiveTransactionManager
import java.time.Duration


@Configuration
@EnableR2dbcRepositories
@Profile("prod")
class MysqlDatabaseConfig {
    // Ref: https://github.com/mirromutth/r2dbc-mysql, https://github.com/r2dbc/r2dbc-pool
    @Bean
    fun connectionFactory(): ConnectionFactory {
        val connectionFactory = ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "pool")
                .option(ConnectionFactoryOptions.PROTOCOL, "mysql")
                .option(ConnectionFactoryOptions.HOST, "localhost")
                .option(ConnectionFactoryOptions.PORT, 3306)
                .option(ConnectionFactoryOptions.USER, "root")
                .option(ConnectionFactoryOptions.PASSWORD, "devpassword")
                .option(ConnectionFactoryOptions.DATABASE, "mydb")
                .build()
        )
        val poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
            .maxSize(20)
            .initialSize(10)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(30))
            .validationQuery("SELECT 1")
            .validationDepth(ValidationDepth.LOCAL)
            .build()

        return ConnectionPool(poolConfig)
    }
//    fun connectionFactory(): ConnectionFactory = ConnectionFactories.get(
//        ConnectionFactoryOptions.builder()
//            .option(ConnectionFactoryOptions.SSL, true)
//            .option(ConnectionFactoryOptions.DRIVER, "pool")
//            .option(ConnectionFactoryOptions.PROTOCOL, "mysql")
//            .option(ConnectionFactoryOptions.HOST, "localhost")
//            .option(ConnectionFactoryOptions.PORT, 3306)
//            .option(ConnectionFactoryOptions.USER, "root")
//            .option(ConnectionFactoryOptions.PASSWORD, "devpassword")
//            .option(ConnectionFactoryOptions.DATABASE, "mydb")
//            .build()
//    )

    @Bean
     fun initializeTable(
        @Qualifier("connectionFactory")
        connectionFactory: ConnectionPool
    ): ConnectionFactoryInitializer {
        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)

        initializer.setDatabasePopulator(
            ResourceDatabasePopulator(
                ClassPathResource("migrations/V1__init.sql")
            )
        )

        return initializer
    }
//    fun initializeTable(
//        @Qualifier("connectionFactory")
//        connectionFactory: ConnectionFactory
//    ): ConnectionFactoryInitializer {
//        val initializer = ConnectionFactoryInitializer()
//        initializer.setConnectionFactory(connectionFactory)
////        initializer.setDatabasePopulator(ResourceDatabasePopulator(ClassPathResource("migrations/V1__init.sql")))
//        return initializer
//    }

}