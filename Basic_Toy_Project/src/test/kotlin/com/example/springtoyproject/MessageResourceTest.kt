package com.example.springtoyproject

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*

internal class MessageResourceTest {

    @BeforeEach
    fun setUp() {
        println("Set Up")
    }

    @AfterEach
    fun tearDown() {
        println("Tear Down")
    }
}