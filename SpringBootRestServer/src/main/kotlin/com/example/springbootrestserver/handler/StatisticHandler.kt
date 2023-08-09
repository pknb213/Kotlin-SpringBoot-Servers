package com.example.springbootrestserver.handler

import com.example.springbootrestserver.domain.statistic.StatisticDto
import com.example.springbootrestserver.domain.statistic.StatisticRepository
import com.example.springbootrestserver.domain.statistic.toDto
import com.example.springbootrestserver.domain.statistic.toEntity
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Component
class StatisticHandler (
    private val statisticRepository: StatisticRepository
){
    suspend fun add(req: ServerRequest): ServerResponse {
        val receivedTravel = req.awaitBodyOrNull(StatisticDto::class)
        return receivedTravel?.let {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(
                    statisticRepository
                        .save(it.toEntity())
                        .toDto()
                )
        } ?: ServerResponse.badRequest().buildAndAwait()
    }
}