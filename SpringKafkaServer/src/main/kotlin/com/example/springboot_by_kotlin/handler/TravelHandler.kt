package com.example.springboot_by_kotlin.handler

import com.example.springboot_by_kotlin.domain.travel.TravelDto
import com.example.springboot_by_kotlin.domain.travel.TravelRepository
import com.example.springboot_by_kotlin.domain.travel.toDto
import com.example.springboot_by_kotlin.domain.travel.toEntity
import org.springframework.http.MediaType
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import java.time.LocalDateTime

@Component
class TravelHandler (
    private val travelRepository: TravelRepository
){
    suspend fun getAll(req: ServerRequest): ServerResponse {
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(
                travelRepository.findAll().map { it.toDto() }
            )
    }

    suspend fun getById(req: ServerRequest): ServerResponse {
        val id = Integer.parseInt(req.pathVariable("id"))
        val existingTravel = travelRepository.findById(id.toLong())

        return existingTravel?.let {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(it)
        } ?: ServerResponse.notFound().buildAndAwait()
    }

    suspend fun add(req: ServerRequest): ServerResponse {
        // Todo: 날짜 포맷 exception message 추가?
        val receivedTravel = req.awaitBodyOrNull(TravelDto::class)
        return receivedTravel?.let {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(
                    travelRepository
                        .save(it.toEntity())
                        .toDto()
                )
        } ?: ServerResponse.badRequest().buildAndAwait()
    }

    suspend fun update(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")

        val receivedTravel = req.awaitBodyOrNull(TravelDto::class)
            ?: return ServerResponse.badRequest().buildAndAwait()

        val existingTravel = travelRepository.findById(id.toLong())
            ?: return ServerResponse.notFound().buildAndAwait()

        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(
                travelRepository.save(
                    receivedTravel.toEntity().copy(
                        id = existingTravel.id,
                        created_date = existingTravel.created_date,
                        updated_date = LocalDateTime.now()
                    )
                ).toDto()
            )
    }

    suspend fun delete(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")

        return if (travelRepository.existsById(id.toLong())) {
            travelRepository.deleteById(id.toLong())
            ServerResponse.noContent().buildAndAwait()
        } else {
            ServerResponse.notFound().buildAndAwait()
        }
    }
}