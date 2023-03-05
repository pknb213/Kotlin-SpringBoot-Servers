package com.example.springbootrestserver.util

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

//@Configuration
//@EnableR2dbcRepositories
//@Profile("prod")
class PostgreConfig {
    /**
     * POSTGRES_USER: developer
     * POSTGRES_PASSWORD: devpassword
     * POSTGRES_DB: developer
     *
     * PG_MAJOR: 15
     *
     * Port: 127.0.0.1:5432
     * */
}