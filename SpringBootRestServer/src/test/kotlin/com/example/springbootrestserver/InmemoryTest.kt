package com.example.springbootrestserver

import com.example.springbootrestserver.domain.city.*
import com.example.springbootrestserver.domain.travel.Travel
import com.example.springbootrestserver.domain.travel.TravelRepository
import com.example.springbootrestserver.domain.statistic.Statistic
import com.example.springbootrestserver.domain.statistic.StatisticRepository
import com.example.springbootrestserver.domain.travel.TravelDto
import com.example.springbootrestserver.domain.travel.toDto
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InmemoryTest (
    @Autowired val client: WebTestClient,
    @Autowired val cityRepository: CityRepository,
    @Autowired val travelRepository: TravelRepository,
    @Autowired val statisticRepository: StatisticRepository
){
    private fun aCity(
        name: String = "서울",
        created_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
        updated_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
    ) = City(
        name = name,
        created_date = created_date,
        updated_date = updated_date
    )

    private fun anotherCity(
        name: String = "부산",
        created_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
        updated_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
    ) = aCity(
        name = name,
        created_date = created_date,
        updated_date = updated_date
    )

    private fun aStatistic(
        city_id: Long = 1
    ) = Statistic(
        city_id = city_id,
        accessed_date = LocalDateTime.now()
    )

    private fun aTravel(
        name: String = "Test Travel",
        city_id: Long = 1,
        start_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
        end_date: LocalDateTime = LocalDateTime.of(2022, 10, 15, 0,0, 0,0),
        created_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
        updated_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
    ) = Travel(
        name = name,
        city_id = city_id,
        start_date = start_date,
        end_date = end_date,
        created_date = created_date,
        updated_date = updated_date
    )

    private fun anotherTravel(
        name: String = "Another Travel",
        city_id: Long = 2,
        start_date: LocalDateTime = LocalDateTime.of(2022, 11, 12, 0,0, 0,0),
        end_date: LocalDateTime = LocalDateTime.of(2022, 11, 15, 0,0, 0,0),
        created_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
        updated_date: LocalDateTime = LocalDateTime.of(2022, 10, 12, 0,0, 0,0),
        ) = aTravel(
        name = name,
        city_id = city_id,
        start_date = start_date,
        end_date = end_date,
        created_date = created_date,
        updated_date = updated_date
    )

    private fun CityRepository.seed(vararg citys: City) =
        runBlocking {
            cityRepository.saveAll(citys.toList()).toList()
        }

    private fun TravelRepository.seed(vararg travels: Travel) =
        runBlocking {
            travelRepository.saveAll(travels.toList()).toList()
        }

    private fun StatisticRepository.seed(vararg statistic: Statistic) =
        runBlocking {
            statisticRepository.saveAll(statistic.toList()).toList()
        }

    @AfterEach
    fun afterEach() {
        runBlocking {
            statisticRepository.deleteAll()
            travelRepository.deleteAll()
            cityRepository.deleteAll()
        }
    }

    @Test
    fun `Retrieve all citys`() {
        cityRepository.seed(aCity(), anotherCity())

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
    fun `Retrieve cat by existing id`() {
        cityRepository.seed(aCity())

        client
            .get()
            .uri("/api/citys/1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<CityDto>()
            .isEqualTo(anotherCity().toDto())
    }

    @Test
    fun `Retrieve cat by non-existing id`() {
        client
            .get()
            .uri("/api/citys/2")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `Add a new city`() {
        client
            .post()
            .uri("/api/citys")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(aCity().toDto())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<CityDto>()
            .isEqualTo(aCity().toDto())
    }

    @Test
    fun `Add a new city with empty request body`() {
        client
            .post()
            .uri("/api/citys")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("{}"))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `Update a city`() {
        cityRepository.seed(aCity(), anotherCity())

        val updatedCity = City(id = 1, name = "New name").toDto()

        client
            .put()
            .uri("/api/citys/2")
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
        val updatedCat = City(id = 1, name = "New name").toDto()

        client
            .put()
            .uri("/api/citys/2")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedCat)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `Update city with empty request body id`() {
        client
            .put()
            .uri("/api/citys/2")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("{}"))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `Delete city with existing id`() {
        // Todo: 수정필요
        cityRepository.seed(aCity(), anotherCity())


        client
            .delete()
            .uri("/api/citys/")
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    fun `Delete cat by non-existing id`() {
        client
            .delete()
            .uri("/api/citys/2")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `Retrieve all travels`() {
        travelRepository.seed(aTravel(), anotherTravel())

        client
            .get()
            .uri("/api/travels")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<TravelDto>()
            .hasSize(2)
            .contains(aTravel().toDto(), anotherTravel().toDto())
    }

    @Test
    fun `Retrieve travel by existing id`() {
        travelRepository.seed(aTravel(), anotherTravel())

        client
            .get()
            .uri("/api/travels/1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TravelDto>()
            .isEqualTo(anotherTravel().toDto())
    }

    @Test
    fun `Retrieve travel by non-existing id`() {
        client
            .get()
            .uri("/api/travels/2")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `Add a new travel`() {
        client
            .post()
            .uri("/api/travels")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(aTravel().toDto())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TravelDto>()
            .isEqualTo(aTravel().toDto())
    }

    @Test
    fun `Add a new travel with empty request body`() {
        client
            .post()
            .uri("/api/travels")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("{}"))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `Update a travel`() {
        travelRepository.seed(aTravel(), anotherTravel())

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
    fun `Update travel with non-existing id`() {
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
    fun `Update travel with empty request body id`() {
        client
            .put()
            .uri("/api/travels/1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("{}"))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `Delete travel with existing id`() {
        travelRepository.seed(aTravel(), anotherTravel())

        client
            .delete()
            .uri("/api/travels/1")
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    fun `Delete travel by non-existing id`() {
        client
            .delete()
            .uri("/api/travels/2")
            .exchange()
            .expectStatus()
            .isNotFound
    }

}
