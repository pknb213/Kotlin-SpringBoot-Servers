# User-Payment REST Server
## 목차
- [개발 환경](#개발-환경)
- [빌드 및 실행하기](#빌드-및-실행하기)
- [기능 요구사항](#기능-요구사항)
- [개발과정](#개발과정)
- [Project Explain](#Project-Explain)

---

## 개발 환경
- 기본 환경
    - IDE: IntelliJ IDEA Ultimate
    - OS: Mac OS M1 Chip, Monterey 12.6
- Server
    - Java 17
    - Spring Boot 2.7.5
    - Gradle
    - MongoDB 5.0
    - Reactor (WebFlux)

## 빌드 및 실행하기
### 터미널 환경
- Java 및 JVM 는 설치되어 있다고 가정한다.
- Mongo 설치
  ```http request
  $ docker pull mongo:5.0
  $ docker run --name mongodb-container -v ~/data:/data/db -d -p 27017:27017 mongo
  ```
  
- Project 실행
  ```
  $ ./gradlew clean build
  $ java -jar build/libs/payment_mini_project-0.0.1-SNAPSHOT.jar 
  ```

- 접속 Base URI: `http://localhost:8080`
- Mongo Database Name: `test`
- Mongo Collections: `account, group, payment`


## 데이터 포맷
### 결제 데이터 포맷

| Field | Type | Description |
| --- | --- | --- |
| paymentId | number | 결제 ID |
| accountId | number | 결제자 ID |
| amount | number | 결제 금액 |
| methodType | string | 결제 수단 (카드, 송금) |
| itemCategory | string | 상품 카테고리 (식품, 뷰티, 스포츠, 도서, 패션) |
| region | string | 결제가 발생한 지역 (서울, 부산, 대구, 인천, 광주, 대전, 울산, 세종, 경기, 강원, 충북, 충남, 전북, 전남, 경북, 경남, 제주) |

### 결제 데이터 샘플

| paymentId | accountId | amount | methodType | itemCategory | region |
| --- | --- | --- | --- | --- | --- |
| 1 | 12 | 456,600 | 송금 | 식품 | 광주 |
| 2 | 12 | 830,800 | 카드 | 스포츠 | 전북 |
| 3 | 21 | 384,800 | 송금 | 식품 | 광주 |
| 4 | 7 | 578,200 | 카드 | 도서 | 충북 |
| 5 | 12 | 945,100 | 송금 | 패션 | 경북 |

### 계정 데이터 포맷

| Field | Type | Description |
| --- | --- | --- |
| accountId | number | 결제자 ID |
| residence | string | 거주 지역 (서울, 부산, 대구, 인천, 광주, 대전, 울산, 세종, 경기, 강원, 충북, 충남, 전북, 전남, 경북, 경남, 제주) |
| age | number | 나이 |

### 계정 데이터 샘플

| accountId | residence | age |
| --- | --- | --- |
| 1 | 대전 | 57 |
| 2 | 강원 | 13 |
| 3 | 강원 | 19 |
| 4 | 광주 | 38 |
| 5 | 전남 | 40 |

### 결제 그룹 데이터 포맷

| Field | Type | Description |
| --- | --- | --- |
| groupId | number | 그룹 ID |
| condition | string | 그룹 조건 |
| description | string | 그룹 설명 |

### 결제 그룹 데이터 샘플
- 조건 설명: `결제자의 거주 지역 외에서의 송금`

```
[
    {
        key: methodType,
        operator: equals,
        value: SEND
    },
    {
        key: residence,
        operator: not equals,
        // 수신된 결제 내역 값을 연산 파라미터로 활용 가능
        value: $region
    }
]
```

## 기능 요구사항
### 필수 기능
- 결제 내역을 받을 수 있는 API 
  - ex) 결제 내역 수신 API 
    ```python
    POST /payment
    {
        "paymentId": 1,
        "accountId": 1,
        "amount": 3000,
        "methodType": "카드",
        "itemCategory": "식품",
        "region": "서울"
    }
    
    200 OK
    {
        "success": true,
        "errorMessage": null
    }
    ```
- 집계된 그룹 관리가 가능한 API
  - ex) 그룹 데이터 등록 API
    ```python
    POST /group
    {
        description: 제주 지역에서 결제,
        condition: [{
            key: residence,
            operator: equals,
            value: '제주'
        }]
    }
    
    200 OK
    {
        "success": true,
        "errorMessage": null
    }
    ```
    
  - ex) 전체 그룹 데이터 조회 API
    ```python
    GET /group
    
    200 OK
    [
        {
            groupId: 1
            description: 제주지역에서 결제
            condition: {GROUP_CONDITION}
        },
        {
            groupId: 2
            description: 10,000원 미만의 카드결제
            condition: {GROUP_CONDITION}
        },
        ...
    ]
    ```
    
  - ex) 특정 그룹 데이터 조회 API
    ```python
    GET /group?groupId={GROUP_ID}
    
    200 OK
    [
        {
            "groupId": 1,
            "description": 제주 지역에서 결제,
            "condition": {GROUP_CONDITION}
        }
    ]
    ```
  - ex) 그룹 데이터 삭제 API
    ```python
    DELETE /group?groupId={GROUP_ID}
    
    200 OK
    {
        "success": true,
        "errorMessage": null
    }
    ```
- 그룹 집계 데이터 조회 API
  - ex) 특정 그룹 집계 데이터 조회 API
    ```python
    GET /statistics?groupId=1
    
    200 OK
    {
        "groupId": "1",
        "count": 5,
        "totalAmount": 384300,
        "avgAmount": 76860,
        "minAmount": 3000,
        "maxAmount": 225000
    }
    ```

## Project Explain

### Structure
프로젝트의 구조는 DDD(Domain Driven Design) 형태로 구성했다.
디렉토리는 크게 Domain 별로 나눠진 Model과 Repository과 Route가 참조하는 Handler 그리고 제공하는 서비스가 구현된 Service로 구성된다. 
```
├── README.md
├── build.gradle.kts
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── src
│   ├── main
│   │   ├── kotlin
│   │   │   └── com
│   │   │       └── example
│   │   │           └── payment_mini_project
│   │   │               ├── PaymentMiniProjectApplication.kt
│   │   │               ├── domain
│   │   │               │   ├── account
│   │   │               │   │   ├── Account.kt
│   │   │               │   │   └── AccountRepository.kt
│   │   │               │   ├── group
│   │   │               │   │   ├── Group.kt
│   │   │               │   │   └── GroupRepository.kt
│   │   │               │   └── payment
│   │   │               │       ├── Payment.kt
│   │   │               │       └── PaymentRepository.kt
│   │   │               ├── handler
│   │   │               │   ├── AccountHandler.kt
│   │   │               │   ├── CsvFileUploadHandler.kt
│   │   │               │   ├── GroupHandler.kt
│   │   │               │   └── PaymentHandler.kt
│   │   │               ├── mongoConfig
│   │   │               │   └── MongoConfig.kt
│   │   │               ├── routes.kt
│   │   │               ├── service
│   │   │               │   ├── AccountService.kt
│   │   │               │   ├── CsvFileUploadService.kt
│   │   │               │   ├── GroupService.kt
│   │   │               │   └── PaymentService.kt
│   │   │               └── utils
│   │   │                   ├── Condition.kt
│   │   │                   └── Response.kt
│   │   └── resources
│   │       └── application.properties
│   └── test
│       ├── kotlin
│       │   └── com
│       │       └── example
│       │           └── payment_mini_project
│       │               ├── PaymentMiniProjectApplicationTests.kt
│       │               ├── domain
│       │               │   ├── AccountTest.kt
│       │               │   ├── GroupTest.kt
│       │               │   └── PaymentTest.kt
│       │               └── handler
│       │                   ├── AccountHandlerTest.kt
│       │                   ├── CSVHandlerTest.kt
│       │                   ├── GroupHandlerTest.kt
│       │                   └── PaymentHandlerTest.kt
│       └── resources
│           └── dummyCsv
│               ├── accounts.csv
│               ├── groups.csv
│               └── payments.csv
└── test.http
```
### 1. 데이터 파일(`.csv`)에서 각 레코드를 데이터베이스에 저장
- Request

```http request
GET http://localhost:8080/csv?<file>
```

- CSV 파일 요청을 받는 핸들러 및 서비스 구현
    - `/csv` URL 요청을 처리하는 `csvFileUploadHandler`와 `csvFileUploadService` 클래스 구현
    - `queryParam("file")`을 통해 `accounts, payments, groups` 값을 사용하여 resource/dummyCsv/.csv CSV 파일을 읽도록 함
    - `main/resource` 내부에는 해당 csv 파일들은 존재하지 않으며, `test/resource`에만 존재한다.
    
### 2. 결제 내역을 받을 수 있는 API
- Request
```http request
POST http://localhost:8080/payment
```
- `Payment` Entity를 `insert`하는 것으로 구현되어 있다.

### 3. 집계된 그룹관리가 가능한 API
- Request

```http request
GET http://localhost:8080/group
POST http://localhost:8080/group
DELETE http://localhost:8080/group?<groupId>
```
- Group 컬렉션에 대한 Create, Read, Delete 구현
  - `GET http://localhost:8080/group` 전체 `Group` 컬렉션의 Document를 반환한다.
  - `GET http://localhost:8080/group?groupId=1,2,3,4` `groupId를` 리스트로 받아 반환한다.
  - `Group` Entity를 `insert`, `findOne`, `findAll`, `delete`로 구현되어 있다.

### 4. 특정 그룹의 집계 데이터를 조회할 수 있는 API
- Request

```http request
GET http://localhost:8080/statistics?<groupId>
```

- 집계 API는 아래 두 가지의 Service로 구현되어 있다. 
  - `fun decodeCondition()`는 string으로 저장된 `conditions을` 정규표현식으로 `key, operation, value`로 다음 서비스에게 전달한다.
  - `fun aggregateByConditions()`는 주어진 `condition에` 따라 Pipeline 생성하여 Mongo의 `Aggregate()` 메서드를 실행하여 결과를 반환한다.
- Aggregate Pipeline
  1. **_Match_**: `Payment` Entity의 필드가 `condition`에 존재하면 추가하여 Filter 역할을 한다.
  2. **_Lookup_**: `Account` Entity의 필드가 `condition`에 존재하면 추가하여 `Payment`의 `accountId`와 `Account`의 `accountId`를 매핑하여 Join한다.
  3. **_Match_**: 2단계 파이프라인이 추가됬을 경우 해당 `Account` Entity의 필드를 Filter 역할을 한다.
  4. **_Group_**: `Payment` Entity의 amount 필드를 `min, max, avg, sum, count` 연산으로 그룹화한다.
  5. **_Project_**: `Response` 형태에 맞게 형태를 변경한다.

