package com.example.spring_multi_module.domain.test.annotation

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@Tag("unitTest")
@Retention(AnnotationRetention.RUNTIME)
annotation class UnitTest()