package com.example.springbootrestserver.domain.city

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CityRepository: CoroutineCrudRepository<City, Long> {
    override fun findAll(): Flow<City>
    override suspend fun findById(id: Long): City?
    override suspend fun existsById(id: Long): Boolean
    override suspend fun <S : City> save(entity: S): City
    override suspend fun deleteById(id: Long)

    // 여행 중인 도시: 여행 시작 일이 빠른 순
    @Query("""
        SELECT * FROM city
        INNER JOIN travel as t ON city.id = t.city_id
        WHERE t.start_date < now() AND t.end_date > now()
        ORDER BY t.start_date ASC;
    """)
    fun findCityByTraveling(): Flow<City>

    // 여행이 예정된 도시: 여행 시작 일이 가까운 순
    @Query("""
        SELECT c.id, c.name, c.created_date, c.updated_date, t.start_date 
        FROM city as c
        INNER JOIN travel as t
        ON t.id = (
            SELECT id
            FROM travel as t2
            WHERE t2.city_id = c.id AND t2.start_date > now()
            ORDER BY t2.start_date ASC 
            LIMIT 1
        )
        ORDER BY t.start_date ASC
        LIMIT 10;
    """)
    fun plannedCityToTravel(): Flow<City>

    // 하루 이내에 등록된 도시: 가장 최근에 등록한 것 부터
    @Query("""
        SELECT * FROM city
        WHERE created_date 
        BETWEEN DATE_ADD(now(), INTERVAL - 1 DAY) AND NOW() AND id NOT IN (:citys)
        ORDER BY created_date DESC
        LIMIT 10;
    """)
    fun registeredCityWithinDay(citys: String): Flow<City>

    // 최근 일주일 이내에 한 번 이상 조회된 도시: 가장 최근에 조회된 것 부터
    @Query("""
        SELECT * FROM city as c
        INNER JOIN statistic as s
        ON s.id = (
            SELECT id
            FROM statistic as s2
            WHERE s2.city_id = c.id
            ORDER BY s2.accessed_date DESC 
            LIMIT 1
        )
        WHERE s.accessed_date 
            BETWEEN DATE_ADD(now(), INTERVAL - 1 WEEK) AND NOW() AND c.id NOT IN (:citys)
        ORDER BY s.accessed_date DESC
        LIMIT 10;
    """)
    fun viewedCityOnceWithinLastWeek(citys: String): Flow<City>

    // 위의 조건에 해당하지 않는 무작위 도시: 무작위
    @Query("""
        SELECT * FROM city as a
        JOIN (
            SELECT id FROM city
            WHERE id NOT IN (:citys)
            ORDER BY rand() LIMIT 10
        ) as b 
        ON a.id = b.id;
    """)
    fun randomCityThatDoNotMeetConditions(citys: Set<Long>): Flow<City>
}