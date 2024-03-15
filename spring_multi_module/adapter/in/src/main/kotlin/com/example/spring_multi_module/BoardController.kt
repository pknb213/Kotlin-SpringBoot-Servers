package com.example.spring_multi_module

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/board")
class BoardController(

) {
    @GetMapping
    fun getBoard(

    ) {
        println("getBoard")
    }

    @PostMapping
    fun postBoard(

    ) {
        println("postBoard")
    }

    @PutMapping
    fun putBoard(

    ) {
        println("putBoard")
    }

    @DeleteMapping
    fun deleteBoard(

    ) {
        println("deleteBoard")
    }
}