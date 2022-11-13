# City-Travel REST Server
## 목차
- [개발 환경](#개발-환경)
- [빌드 및 실행하기](#빌드-및-실행하기)
- [데이터 포맷](#데이터-포맷)
- [API](#API)
- [요구 사항](#요구-사항)
- [Unit Test](#Unit-Test)

---

## 개발 환경
- 기본 환경
  - IDE: IntelliJ IDEA Ultimate
  - OS: Mac OS M1 Chip, Monterey 12.6
- Server
  - Java 17
  - Spring Boot 2.7.5
  - Gradle
  - Reactor (WebFlux)
  - Kotlin-coroutine
  - R2DBC
  - Mock
- Database
  - Mysql 8.0
  - H2

## 빌드 및 실행하기
### 터미널 환경
- `Java` 및 `JVM`은 설치되어 있다고 가정한다.

- Server
  - Port: `8080`
  - Profile: `test, dev, prod`
  
- Project 실행
  ```
  $ docker-compose up -d
  $ ./gradlew clean build
  $ java -jar -Dspring.profiles.active={{PROFILE}} build/libs/SpringBoot_By_Kotlin-0.0.1-SNAPSHOT.jar 
  ```

- Mysql
  - 위와 같이 Docker-compose로 설치.
  - Url: `r2dbc:pool:mysql://localhost:24000/mydb?useUnicode=true&characterEncoding=utf8`
  - Port: `24000`
  - DB: `mydb`
  - Username: `root`
  - Password: `devpassword`

- H2
  - 별도 설치 필요 없음.
  - Url: `r2dbc:h2:mem://localhost/testdb;MODE=MYSQL;DATABASE_TO_LOWER=true;DATABASE_TO_UPPER=false`
  - DB: `testdb`
  - Username: `sa`
  - Password:

## 데이터 포맷
프로젝트를 실행하면 우선적으로 `Profile에` 따라 `resources/migrations` 내부에 있는
`V1__init.sql` 파일을 실행하여 테이블을 초기화합니다.

### City Format
도시 관련 테이블입니다.

| Field        | Type          | Description |
|--------------|---------------|-------------|
| id           | Long          | ID          |
| name         | String        | 도시 이름       |
| created_date | LocalDateTime | 생성 날짜       |
| updated_date | LocalDateTime | 업데이트 날짜     |

### Travel Format
여행 관련 테이블입니다.

| Field        | Type          | Description |
|--------------|---------------|-------------|
| id           | Long          | ID          |
| name         | String        | 여행 이름       |
| city_id      | Long          | 도시 Id       |
| start_date   | LocalDateTime | 여행 시작 날짜    |
| end_date     | LocalDateTime | 여행 마무리 날짜   |
| created_date | LocalDateTime | 생성 날짜       |
| updated_date | LocalDateTime | 업데이트 날짜     |

### Statistic Format
도시 조회 API에 이용되는 테이블로 도시가 조회되면 해당 도시의 ID와 날짜를 저장합니다. 

| Field         | Type          | Description |
|---------------|---------------|-------------|
| id            | Long          | ID          |
| city_id       | Long          | 도시 Id       |
| accessed_date | LocalDateTime | 조회 날짜       |

## API
API Url은 Routes 파일에 작성했으며 아래와 같습니다.
- 도시 관련 API 입니다.
  - 도시 등록: `POST /api/citys`
  - 도시 전체 조회: `GET /api/citys`
  - 단일 도시 조회: `GET /api/citys/<id>`
  - 도시 수정: `PUT /api/citys/<id>`
  - 도시 삭제: `DELETE /api/citys/<id>`
  - 사용자별 도시 목록 조회: `GET /api/by/users`

- 여행 관련 API 입니다.
  - 여행 등록: `POST /api/travels`
  - 여행 전체 조회: `GET /api/travels`
  - 단일 여행 조회: `GET /api/travels/<id>`
  - 여행 수정: `PUT /api/travels/<id>`
  - 여행 삭제: `DELETE /api/travels/<id>`

## 기능 사항
- 도시 삭제 API
  - 조건: 해당 도시가 지정된 여행이 없을 경우만 삭제 가능
  - 외래키 추가: `FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE RESTRICT,`

- 사용자별 도시 목록 조회 API
  - `domain/city/CityRepository`에 해당 도시 노출을 호출하는 Query로 작성했습니다.
  - 마지막 조건에 해당하지 않은 도시를 구현하기 위해 `handler/CityHandler`에서 해당 도시 노출을 수행하면서 City의 객체 Id를 저장하고 해당 쿼리를 수행할 때 참조합니다.

## Unit Test
Acceptance 테스트는 test 디렉토리 내부에 다음 두 파일으로 존재합니다.
- `MockedCityRepositoryIntegrationTest`
- `MockedTravelRepositoryIntegrationTest`

추가로 개발하면서 간단한 Http Request는 최상위 디렉토리에 `test.http`에 서술되어 있습니다.


## 사용자별 도시 목록 조회 API
CRUD 다른 API 외, 아래 사용자별 도시 목록 조회 API에 대해 입출력 결과입니다.
- 사용자별 도시 목록 조회: `GET /api/by/users`

- City Table
- `20`개의 `도시`가 입력되어 있으며, `1~10`과 `11~20` `created_date`가 다릅니다.
- <img src="https://user-images.githubusercontent.com/36396206/201536843-f18c4ce4-7944-4b94-b5e9-c4db69dc455a.png" width="50%" height="50%"/>

- Travel Table
- `20`개의 `여행`이 입력되어 있으며, 각각 `city_id, start_date 및 end_date`가 다릅니다.
- <img src="https://user-images.githubusercontent.com/36396206/201536863-037bcb11-9d17-42a5-8373-679084194ef7.png" width="50%" height="50%"/>

- Statistic Table
- `1~7번` `City`가 `1번 이상` 조회되었으며, `7>6=5=4>3>2>1` 도시 순서로 많이 조회되었습니다.
- <img src="https://user-images.githubusercontent.com/36396206/201536864-266fef7e-ef36-4f4e-9410-05afd649fea5.png" width="50%" height="50%"/>

### 결과는 아래와 같습니다.
```
{
  "여행 중인 도시: 여행 시작일이 빠른 순": [
    {
      "id": 2,
      "name": "서울",
      "created_date": "2022-11-14T02:39:15",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 7,
      "name": "제주",
      "created_date": "2022-11-14T02:40:27",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 11,
      "name": "부산",
      "created_date": "2022-11-14T02:41:26",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 18,
      "name": "울산",
      "created_date": "2022-11-14T02:42:24",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 17,
      "name": "인천",
      "created_date": "2022-11-14T02:42:11",
      "updated_date": "2022-11-13T17:53:19"
    }
  ],
  "여행이 예정된 도시: 여행 시작일이 가까운 순": [
    {
      "id": 1,
      "name": "서울",
      "created_date": "2022-11-13T02:37:55",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 2,
      "name": "부산",
      "created_date": "2022-11-13T02:37:58",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 7,
      "name": "제주",
      "created_date": "2022-11-13T02:38:11",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 3,
      "name": "대전",
      "created_date": "2022-11-13T02:38:00",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 6,
      "name": "울산",
      "created_date": "2022-11-13T02:38:09",
      "updated_date": "2022-11-13T17:53:19"
    }
  ],
  "하루 이내에 등록된 도시: 가장 최근에 등록한 것 부터": [
    {
      "id": 10,
      "name": "전주",
      "created_date": "2022-11-13T02:42:46",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 9,
      "name": "강릉",
      "created_date": "2022-11-13T02:42:43",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 8,
      "name": "포항",
      "created_date": "2022-11-13T02:38:14",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 7,
      "name": "제주",
      "created_date": "2022-11-13T02:38:11",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 6,
      "name": "울산",
      "created_date": "2022-11-13T02:38:09",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 5,
      "name": "인천",
      "created_date": "2022-11-13T02:38:06",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 4,
      "name": "대구",
      "created_date": "2022-11-13T02:38:04",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 3,
      "name": "대전",
      "created_date": "2022-11-13T02:38:00",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 2,
      "name": "부산",
      "created_date": "2022-11-13T02:37:58",
      "updated_date": "2022-11-13T17:53:19"
    }
  ],
  "최근 일주일 이내에 한 번 이상 조회된 도시: 가장 최근에 조회된 것 부터": [
    {
      "id": 4,
      "name": "대구",
      "created_date": "2022-11-13T02:38:04",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 5,
      "name": "인천",
      "created_date": "2022-11-13T02:38:06",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 6,
      "name": "울산",
      "created_date": "2022-11-13T02:38:09",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 7,
      "name": "제주",
      "created_date": "2022-11-13T02:38:11",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 3,
      "name": "대전",
      "created_date": "2022-11-13T02:38:00",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 2,
      "name": "부산",
      "created_date": "2022-11-13T02:37:58",
      "updated_date": "2022-11-13T17:53:19"
    }
  ],
  "위의 조건에 해당하지 않는 무작위 도시: 무작위": [
    {
      "id": 14,
      "name": "태릉",
      "created_date": "2022-10-14T02:43:00",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 17,
      "name": "양양",
      "created_date": "2022-10-14T02:43:11",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 12,
      "name": "부천",
      "created_date": "2022-10-14T02:42:51",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 11,
      "name": "진주",
      "created_date": "2022-10-14T02:42:49",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 20,
      "name": "부평",
      "created_date": "2022-10-14T02:43:33",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 13,
      "name": "파주",
      "created_date": "2022-10-14T02:42:55",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 16,
      "name": "여수",
      "created_date": "2022-10-14T02:43:08",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 18,
      "name": "독도",
      "created_date": "2022-10-14T02:43:19",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 19,
      "name": "광양",
      "created_date": "2022-10-14T02:43:27",
      "updated_date": "2022-11-13T17:53:19"
    },
    {
      "id": 15,
      "name": "강화",
      "created_date": "2022-10-14T02:43:04",
      "updated_date": "2022-11-13T17:53:19"
    }
  ]
}
```