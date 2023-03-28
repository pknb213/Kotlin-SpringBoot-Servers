# ~ Memos ~
아씨 드럽게 
# mysql driver
https://github.com/jasync-sql/jasync-sql/tree/master/samples/spring-kotlin

계층형 Layer
```
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           └── demo
    │   │               ├── DemoApplication.java
    │   │               ├── config
    │   │               ├── controller
    │   │               ├── dao
    │   │               ├── domain
    │   │               ├── exception
    │   │               └── service
    │   └── resources
    │       └── application.properties

```

도메인형 Domain
```
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           └── demo
    │   │               ├── DemoApplication.java
    │   │               ├── coupon
    │   │               │   ├── controller
    │   │               │   ├── domain
    │   │               │   ├── exception
    │   │               │   ├── repository
    │   │               │   └── service
    │   │               ├── member
    │   │               │   ├── controller
    │   │               │   ├── domain
    │   │               │   ├── exception
    │   │               │   ├── repository
    │   │               │   └── service
    │   │               └── order
    │   │                   ├── controller
    │   │                   ├── domain
    │   │                   ├── exception
    │   │                   ├── repository
    │   │                   └── service
    │   └── resources
    │       └── application.properties

```

# 전체적인 구조
```
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── spring
    │   │           └── guide
    │   │               ├── ApiApp.java
    │   │               ├── SampleApi.java
    │   │               ├── domain
    │   │               │   ├── coupon
    │   │               │   │   ├── api
    │   │               │   │   ├── application
    │   │               │   │   ├── dao
    │   │               │   │   ├── domain
    │   │               │   │   ├── dto
    │   │               │   │   └── exception
    │   │               │   ├── member
    │   │               │   │   ├── api
    │   │               │   │   ├── application
    │   │               │   │   ├── dao
    │   │               │   │   ├── domain
    │   │               │   │   ├── dto
    │   │               │   │   └── exception
    │   │               │   └── model
    │   │               │       ├── Address.java
    │   │               │       ├── Email.java
    │   │               │       └── Name.java
    │   │               ├── global
    │   │               │   ├── common
    │   │               │   │   ├── request
    │   │               │   │   └── response
    │   │               │   ├── config
    │   │               │   │   ├── SwaggerConfig.java
    │   │               │   │   ├── properties
    │   │               │   │   ├── resttemplate
    │   │               │   │   └── security
    │   │               │   ├── error
    │   │               │   │   ├── ErrorResponse.java
    │   │               │   │   ├── GlobalExceptionHandler.java
    │   │               │   │   └── exception
    │   │               │   └── util
    │   │               └── infra
    │   │                   ├── email
    │   │                   └── sms
    │   │                       ├── AmazonSmsClient.java
    │   │                       ├── SmsClient.java
    │   │                       └── dto
    │   └── resources
    │       ├── application-dev.yml
    │       ├── application-local.yml
    │       ├── application-prod.yml
    │       └── application.yml
```

### Detail Example
```
domain
│   ├── member
│   │   ├── api
│   │   │   └── MemberApi.java
│   │   ├── application
│   │   │   ├── MemberProfileService.java
│   │   │   ├── MemberSearchService.java
│   │   │   ├── MemberSignUpRestService.java
│   │   │   └── MemberSignUpService.java
│   │   ├── dao
│   │   │   ├── MemberFindDao.java
│   │   │   ├── MemberPredicateExecutor.java
│   │   │   ├── MemberRepository.java
│   │   │   ├── MemberSupportRepository.java
│   │   │   └── MemberSupportRepositoryImpl.java
│   │   ├── domain
│   │   │   ├── Member.java
│   │   │   └── ReferralCode.java
│   │   ├── dto
│   │   │   ├── MemberExistenceType.java
│   │   │   ├── MemberProfileUpdate.java
│   │   │   ├── MemberResponse.java
│   │   │   └── SignUpRequest.java
│   │   └── exception
│   │       ├── EmailDuplicateException.java
│   │       ├── EmailNotFoundException.java
│   │       └── MemberNotFoundException.java
│   └── model
│       ├── Address.java
│       ├── Email.java
│       └── Name.java

```

## Domain
model 디렉터리는 Domain Entity 객체들이 공통적으로 사용할 객체들로 구성됩니다. 대표적으로 Embeddable 객체, Enum 객체 등이 있습니다.

member 디렉터리는 간단한 것들부터 설명하겠습니다.

api : 컨트롤러 클래스들이 존재합니다. 외부 rest api로 프로젝트를 구성하는 경우가 많으니 api라고 지칭했습니다. 
Controller 같은 경우에는 ModelAndView를 리턴하는 느낌이 있어서 명시적으로 api라고 하는 게 더 직관적인 거 같습니다.
domain : 도메인 엔티티에 대한 클래스로 구성됩니다. 특정 도메인에만 속하는 Embeddable, Enum 같은 클래스도 구성됩니다.
dto : 주로 Request, Response 객체들로 구성됩니다.
exception : 해당 도메인이 발생시키는 Exception으로 구성됩니다

## DTO(Data Transfer Object)
- 계층간 데이터 교환을 위한 객체(Java Beans)
- DB의 데이터가 Presentation Logic Tier로 넘어올 때 DTO 모습으로 바꿔 오고간다.
- 로직을 갖지않은 순수한 데이터 객체이며, getter, setter만 갖는다.
- 하지만 DB에서 꺼낸 값을 임의로 변경할 필요가 없기 때문에 DTO 클래스엔 setter가 없으며 생성자에서 값을 할당한다.

## VO(Value Object)
- 특정 비즈니스 값을 담은 객체.
- Vo의 핵심 열할은 equals()와 hashcode()를 오버라이딩 하는 것이다
- Vo 내부에 선언된 필드의 모든 값들이 같아야 똑같은 객체라고 판별한다.

## Application
application 디렉터리는 도메인 객체와 외부 영역을 연결해주는 파사드와 같은 역할을 주로 담당하는 클래스로 구성됩니다. 
대표적으로 데이터베이스 트랜잭션을 처리를 진행합니다. 
service 계층과 유사합니다. 디렉터리 이름을 service로 하지 않은 이유는 service로 했을 경우 
xxxxService로 클래스 네임을 해야 한다는 강박관념이 생기기 때문에 application이라고 명명했습니다.

## Dao(Data Access Object)
- 실제 DB에 접근하는 객체로 Service, DB를 연결하는 고리 역할 담당, SQL를 사용
repository 와 비슷합니다. repository로 하지 않은 이유는 조회 전용 구현체들이 작성 많이 작성되는데 
이러한 객체들은 DAO라는 표현이 더 직관적이라고 판단했습니다. Querydsl를 이용해서 Repository 확장하기(1), (2)처럼 
Reopsitory를 DAO처럼 확장하기 때문에 dao 디렉터리 명이 더 직관적이라고 생각합니다.

# Global
global은 프로젝트 전방위적으로 사용되는 객체들로 구성됩니다. 
global로 지정한 이유는 common, util, config 등 프로젝트 전체에서 사용되는 클래스들이 global이라는 디렉터리에 모여 있는 것이 좋다고 생각했습니다.

common : 공통으로 사용되는 Value 객체들로 구성됩니다. 페이징 처리를 위한 Request, 공통된 응답을 주는 Response 객체들이 있습니다.
config : 스프링 각종 설정들로 구성됩니다.
error : 예외 핸들링을 담당하는 클래스로 구성됩니다. Exception Guide에서 설명했던 코드들이 있습니다.
util : 유틸성 클래스들이 위치합니다.

# Infra
infra 디렉터리는 인프라스트럭처 관련된 코드들로 구성됩니다. 
인프라스트럭처는 대표적으로 이메일 알림, SMS 알림 등 외부 서비스에 대한 코드들이 존재합니다. 
그렇기 때문에 domain, global에 속하지 않습니다. global로 볼 수는 있지만 이 계층도 잘 관리해야 하는 대상이기에 별도의 디렉터리 했습니다.

인프라스트럭처는 대체성을 강하게 갔습니다. SMS 메시지를 보내는 클라이언트를 국내 사용자에게는 KT SMS, 해외 사용자에게는 
Amazon SMS 클라이언트를 이용해서 보낼 수 있습니다.

만약 국내 서비스만 취급한다고 하더라도 언제 다른 플랫폼으로 변경될지 모르니 이런 인프라스트럭처는 기계적으로 인터페이스를 두고 개발하는 것이 좋습니다. 
이런 측면에서 infra 디렉터리로 분리 시켜 관련 코드들을 모았습니다.

