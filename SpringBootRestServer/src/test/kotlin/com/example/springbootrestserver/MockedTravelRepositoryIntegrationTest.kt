package com.example.springbootrestserver

import com.example.springbootrestserver.domain.city.CityRepository
import com.example.springbootrestserver.domain.statistic.StatisticRepository
import com.example.springbootrestserver.domain.travel.Travel
import com.example.springbootrestserver.domain.travel.TravelDto
import com.example.springbootrestserver.domain.travel.TravelRepository
import com.example.springbootrestserver.domain.travel.toDto
import com.example.springbootrestserver.handler.CityHandler
import com.example.springbootrestserver.handler.StatisticHandler
import com.example.springbootrestserver.handler.TravelHandler
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.reactive.function.BodyInserters.fromValue
import java.time.LocalDateTime
import java.util.*


@WebFluxTest
@Import(Routes::class, CityHandler::class, TravelHandler::class, StatisticHandler::class)
class MockedTravelRepositoryIntegrationTest(
    @Autowired private val client: WebTestClient
) {
    @MockkBean
    private lateinit var cityRepository: CityRepository
    @MockkBean
    private lateinit var travelRepository: TravelRepository
    @MockkBean
    private lateinit var statisticRepository: StatisticRepository

    private fun aTravel(
        name: String = "Test Travel",
        city_id: Long = 1,
        start_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
        end_date: LocalDateTime = LocalDateTime.of(2022, 10, 15, 0,0, 0,0),
    ) = Travel(
        name = name,
        city_id = city_id,
        start_date = start_date,
        end_date = end_date,
        created_date = LocalDateTime.now(),
        updated_date = LocalDateTime.now()
    )

    private fun anotherTravel(
        name: String = "Another Travel",
        city_id: Long = 2,
        start_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
        end_date: LocalDateTime = LocalDateTime.of(2022, 10, 15, 0,0, 0,0),
    ) = aTravel(
        name = name,
        city_id = city_id,
        start_date = start_date,
        end_date = end_date,
    )

    @Test
    fun `Retrieve all travels`() {
        every {
            travelRepository.findAll()
        } returns flow {
            emit(aTravel())
            emit(anotherTravel())
        }

        client
            .get()
            .uri("/api/travels")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<TravelDto>() // Todo: Date() Type X
            .hasSize(2)
            .contains(aTravel().toDto(), anotherTravel().toDto())
    }

    @Test
    fun `Retrieve Travel by existing id`() {
        coEvery {
            travelRepository.findById(any())
        } coAnswers {
            aTravel()
        }

        client
            .get()
            .uri("/api/travels/1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TravelDto>()  // Todo: Date() Type X
            .isEqualTo(aTravel().toDto())
    }

    @Test
    fun `Retrieve Travel by non-existing id`() {
        coEvery {
            travelRepository.findById(any())
        } returns null

        client
            .get()
            .uri("/api/travels/2")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `Add a new Travel`() {
        val savedTravel = slot<Travel>()
        coEvery {
            travelRepository.save(capture(savedTravel))
        } coAnswers {
            savedTravel.captured
        }

        client
            .post()
            .uri("/api/travels/")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(aTravel().toDto())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TravelDto>()
            .isEqualTo(savedTravel.captured.toDto())
    }

    @Test
    fun `Add a new Travel with empty request body`() {
        val savedTravel = slot<Travel>()
        coEvery {
            travelRepository.save(capture(savedTravel))
        } coAnswers {
            savedTravel.captured
        }

        client
            .post()
            .uri("/api/travels/")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fromValue("{}"))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `Update a Travel`() {
        coEvery {
            travelRepository.findById(any())
        } coAnswers {
            aTravel()
        }

        val savedTravel = slot<Travel>()
        coEvery {
            travelRepository.save(capture(savedTravel))
        } coAnswers {
            savedTravel.captured
        }

        val updatedTravel = aTravel(name = "New name").toDto()

        client
            .put()
            .uri("/api/travels/1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedTravel)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TravelDto>()
            .isEqualTo(updatedTravel)
    }

    @Test
    fun `Update Travel with non-existing id`() {
        val requestedId = slot<Long>()
        coEvery {
            travelRepository.findById(capture(requestedId))
        } coAnswers {
            nothing
        }

        val updatedTravel = aTravel(name = "New name").toDto()

        client
            .put()
            .uri("/api/travels/2")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedTravel)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `Update Travel with empty request body id`() {
        coEvery {
            travelRepository.findById(any())
        } coAnswers {
            aTravel()
        }

        client
            .put()
            .uri("/api/travels/1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fromValue("{}"))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `Delete Travel with existing id`() {
        coEvery {
            travelRepository.existsById(any())
        } coAnswers {
            true
        }

        coEvery {
            travelRepository.deleteById(any())
        } coAnswers {
            nothing
        }

        client
            .delete()
            .uri("/api/travels/1")
            .exchange()
            .expectStatus()
            .isNoContent

        coVerify { travelRepository.deleteById(any()) }
    }

    @Test
    fun `Delete Travel by non-existing id`() {
        coEvery {
            travelRepository.existsById(any())
        } coAnswers {
            false
        }

        client
            .delete()
            .uri("/api/travels/2")
            .exchange()
            .expectStatus()
            .isNotFound

        coVerify(exactly = 0) { travelRepository.deleteById(any()) }
    }
}

