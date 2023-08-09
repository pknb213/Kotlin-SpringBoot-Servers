package com.example.springboot_by_kotlin

import com.example.springboot_by_kotlin.domain.city.City
import com.example.springboot_by_kotlin.domain.city.CityDto
import com.example.springboot_by_kotlin.domain.city.CityRepository
import com.example.springboot_by_kotlin.domain.city.toDto
import com.example.springboot_by_kotlin.domain.statistic.Statistic
import com.example.springboot_by_kotlin.domain.statistic.StatisticRepository
import com.example.springboot_by_kotlin.domain.travel.TravelRepository
import com.example.springboot_by_kotlin.handler.CityHandler
import com.example.springboot_by_kotlin.handler.StatisticHandler
import com.example.springboot_by_kotlin.handler.TravelHandler
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.reactive.function.BodyInserters.fromValue
import java.time.LocalDateTime
import java.util.*


@ActiveProfiles("test")
@WebFluxTest
@Import(Routes::class, CityHandler::class, TravelHandler::class, StatisticHandler::class)
class MockedCityRepositoryIntegrationTest(
    @Autowired private val client: WebTestClient
) {
    @MockkBean
    private lateinit var cityRepository: CityRepository
    @MockkBean
    private lateinit var travelRepository: TravelRepository
    @MockkBean
    private lateinit var statisticRepository: StatisticRepository

    private fun aCity(
        id: Long = 1,
        name: String = "서울"
//      listOf("서울", "부산", "대전", "대구", "인천", "강릉", "세종").shuffled().asSequence().find { true }!!
    ) = City(
        id = id,
        name = name,
        created_date = LocalDateTime.now(),
        updated_date = LocalDateTime.now()
    )

    private fun anotherCity(
        id: Long = 2,
        name: String = "부산"
    ) = aCity(
        id = id,
        name = name,
    )

    private fun aStatistic(
        id: Long = 1,
        city_id: Long = 1
    ) = Statistic(
        id = id,
        city_id = city_id,
        accessed_date = LocalDateTime.now()
    )


    @Test
    @DisplayName("모든 City Table을 조회한다.")
    fun `Retrieve all citys`() {
        every {
            cityRepository.findAll()
        } returns flow {
            emit(aCity())
            emit(anotherCity())
        }

        client
            .get()
            .uri("/api/citys")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<CityDto>()
            .hasSize(2)
            .contains(aCity().toDto(), anotherCity().toDto())
    }

    @Test
    fun `Retrieve city by existing id`() {
        coEvery {
            cityRepository.findById(any())
        } coAnswers {
            aCity()
        }
        coEvery {
            statisticRepository.save(any())
        } coAnswers {
            aStatistic()
        }

        client
            .get()
            .uri("/api/citys/1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<CityDto>()
            .isEqualTo(aCity().toDto())
    }

    @Test
    fun `Retrieve city by non-existing id`() {
        coEvery {
            cityRepository.findById(any())
        } returns null

        coEvery {
            statisticRepository.save(any())
        } coAnswers {
            aStatistic()
        }

        client
            .get()
            .uri("/api/citys/2")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `Add a new city`() {
        val savedCity = slot<City>()
        coEvery {
            cityRepository.save(capture(savedCity))
        } coAnswers {
            savedCity.captured
        }

        client
            .post()
            .uri("/api/citys/")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(aCity().toDto())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<CityDto>()
            .isEqualTo(savedCity.captured.toDto())
    }

    @Test
    fun `Add a new city with empty request body`() {
        val savedCat = slot<City>()
        coEvery {
            cityRepository.save(capture(savedCat))
        } coAnswers {
            savedCat.captured
        }

        client
            .post()
            .uri("/api/citys/")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fromValue("{}"))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `Update a city`() {
        coEvery {
            cityRepository.findById(any())
        } coAnswers {
            aCity()
        }

        val savedCity = slot<City>()
        coEvery {
            cityRepository.save(capture(savedCity))
        } coAnswers {
            savedCity.captured
        }

        val updatedCity = aCity(name = "New name").toDto()

        client
            .put()
            .uri("/api/citys/1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedCity)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<CityDto>()
            .isEqualTo(updatedCity)
    }

    @Test
    fun `Update city with non-existing id`() {
        val requestedId = slot<Long>()
        coEvery {
            cityRepository.findById(capture(requestedId))
        } coAnswers {
            nothing
        }

        val updatedCity = aCity(name = "New name").toDto()

        client
            .put()
            .uri("/api/citys/2")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedCity)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `Update city with empty request body id`() {
        coEvery {
            cityRepository.findById(any())
        } coAnswers {
            aCity()
        }

        client
            .put()
            .uri("/api/citys/1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fromValue("{}"))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `Delete city with existing id`() {
        coEvery {
            cityRepository.existsById(any())
        } coAnswers {
            true
        }

        coEvery {
            cityRepository.deleteById(any())
        } coAnswers {
            nothing
        }

        client
            .delete()
            .uri("/api/citys/1")
            .exchange()
            .expectStatus()
            .isNoContent

        coVerify { cityRepository.deleteById(any()) }
    }

    @Test
    fun `Delete city by non-existing id`() {
        coEvery {
            cityRepository.existsById(any())
        } coAnswers {
            false
        }

        client
            .delete()
            .uri("/api/citys/2")
            .exchange()
            .expectStatus()
            .isNotFound

        coVerify(exactly = 0) { cityRepository.deleteById(any()) }
    }
}

