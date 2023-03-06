package com.example.springbootrestserver.global.config

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator

@Configuration
@EnableR2dbcRepositories
@Profile("prod")
class MysqlConfig {
    @Bean
    fun connectionFactory(): ConnectionFactory = ConnectionFactories.get(
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.SSL, true)
            .option(ConnectionFactoryOptions.DRIVER, "pool")
            .option(ConnectionFactoryOptions.PROTOCOL, "mysql")
            .option(ConnectionFactoryOptions.HOST, "localhost")
            .option(ConnectionFactoryOptions.PORT, 3306)
            .option(ConnectionFactoryOptions.USER, "root")
            .option(ConnectionFactoryOptions.PASSWORD, "devpassword")
            .option(ConnectionFactoryOptions.DATABASE, "mydb")
            .build()
    )

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