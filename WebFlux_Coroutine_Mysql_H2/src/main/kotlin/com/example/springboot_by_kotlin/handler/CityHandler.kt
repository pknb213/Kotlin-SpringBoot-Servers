package com.example.springboot_by_kotlin.handler

import com.example.springboot_by_kotlin.domain.city.*
import com.example.springboot_by_kotlin.domain.statistic.Statistic
import com.example.springboot_by_kotlin.domain.statistic.StatisticRepository
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties.Server
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.stream.IntStream
import java.util.stream.Stream

@Component
class CityHandler (
    private val cityRepository: CityRepository,
    private val statisticRepository: StatisticRepository
){
    suspend fun getAll(req: ServerRequest): ServerResponse {
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(
                cityRepository.findAll().map { it.toDto() }
            )
    }

    suspend fun getById(req: ServerRequest): ServerResponse {
        val id = Integer.parseInt(req.pathVariable("id"))
        val existingCity = cityRepository.findById(id.toLong())
        statisticRepository.save(Statistic(city_id = id.toLong()))
        return existingCity?.let {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(it)
        } ?: ServerResponse.notFound().buildAndAwait()
    }

    suspend fun add(req: ServerRequest): ServerResponse {
        val receivedCity = req.awaitBodyOrNull(CityDto::class)

        return receivedCity?.let {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(
                    cityRepository
                        .save(it.toEntity())
                        .toDto()
                )
        } ?: ServerResponse.badRequest().buildAndAwait()
    }

    suspend fun update(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")

        val receivedCity = req.awaitBodyOrNull(CityDto::class)
            ?: return ServerResponse.badRequest().buildAndAwait()

        val existingCity = cityRepository.findById(id.toLong())
            ?: return ServerResponse.notFound().buildAndAwait()

        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(
                cityRepository.save(
                    receivedCity.toEntity().copy(
                        id = existingCity.id,
                        created_date = existingCity.created_date,
                        updated_date = LocalDateTime.now()
                    )
                ).toDto()
            )
    }

   suspend fun delete(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id")

        return if (cityRepository.existsById(id.toLong())) {
            try {
                cityRepository.deleteById(id.toLong())
            }catch (err: DataIntegrityViolationException) {
                return ServerResponse.badRequest().bodyValueAndAwait(
                    mapOf(
                        "timestamp" to LocalDateTime.now().toString(),
                        "status" to 400,
                        "errorCode" to 1451,
                        "message" to "Cannot delete or update a parent row"
                    )
                )
            }
            ServerResponse.noContent().buildAndAwait()
        } else {
            ServerResponse.notFound().buildAndAwait()
        }
    }

   suspend fun getByCustom(req: ServerRequest): ServerResponse {
        val alreadyCityIds = mutableListOf<Long>()
        val plannedQryCity = cityRepository.plannedCityToTravel().toList()
        plannedQryCity.map {
            alreadyCityIds.add(it.id!!)
        }
        val registeredQryCity = cityRepository.registeredCityWithinDay(alreadyCityIds.joinToString(",")).toList()
        registeredQryCity.map {
            alreadyCityIds.add(it.id!!)
        }
        val viewedQryCity = cityRepository.viewedCityOnceWithinLastWeek(alreadyCityIds.joinToString(",")).toList()
        viewedQryCity.map {
            alreadyCityIds.add(it.id!!)
        }
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(
                mapOf(
                    "여행 중인 도시: 여행 시작일이 빠른 순" to
                            cityRepository.findCityByTraveling().toList(),
                    "여행이 예정된 도시: 여행 시작일이 가까운 순" to
                            plannedQryCity,
                    "하루 이내에 등록된 도시: 가장 최근에 등록한 것 부터" to
                            registeredQryCity,
                    "최근 일주일 이내에 한 번 이상 조회된 도시: 가장 최근에 조회된 것 부터" to
                            viewedQryCity,
                    "위의 조건에 해당하지 않는 무작위 도시: 무작위" to
                            cityRepository.randomCityThatDoNotMeetConditions(alreadyCityIds.toSet()).toList()
                )
            )
    }
}