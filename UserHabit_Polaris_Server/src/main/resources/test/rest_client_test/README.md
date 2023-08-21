# Installation 'REST Client '
- vscdoe 최신 설치
- extensions > rest client 검색 > 설치 (https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
- ~/.config/Code/User/settings.json 환경설정 파일에 아래 코드 추가 (환경설정에 아래 주석처리된 jwt 값으로 하나 교체하면 간단하게 로그인 된 상태를 바꿀 수 있다.)
```json
{
	...
	...
	...
	"rest-client.environmentVariables": {
		"$shared": {
			"host": "http://localhost:8000/v3",
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6InRlc3QwMUB1c2VyaGFiaXQuaW8iLCJpYXQiOjE2Mjk4MDUxMDEsImV4cCI6MTYzMjM5NzEwMSwiYWlsIjpbImU2YzEwMWYwMjBlMTAxOGI1YmExN2NkYmUzMmFkZTJkNjc5YjQ0YmMiLCJlNmMxMDFmMDIwZTEwMThiNWJhMTdjZGJlMzJhZGUyZDY3OWI0NGJjXzIiXSwibHZsIjoyNTV9.Cw2u61AKq0hBjFiFNL2LSwxmw9A0ghByP4-eUfKP5xY", // test01@userhabit.io
			"jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6InN5c3RlbS5yd3VkQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5NjQ0NCwiZXhwIjoxNjMzMDg4NDQ0LCJhaWwiOltdLCJsdmwiOjE0M30.CHwFbQAUO_ksU8KpMd_baLgVUo5GL1pP2YkE6ySNgTg", // system.rwud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6InN5c3RlbS5fd3VkQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5NjU0OCwiZXhwIjoxNjMzMDg4NTQ4LCJhaWwiOltdLCJsdmwiOjE0Mn0.V1caDtaoWSjGHXy0OKXNLwLZJjrS165Fx0JOHZp0VP8", // system._wud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6InN5c3RlbS5yX3VkQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5NjU4NSwiZXhwIjoxNjMzMDg4NTg1LCJhaWwiOltdLCJsdmwiOjE0MX0.BWSPKwuTS9ioVHJnrlvPpX13hpaiMQiOKE1usJg3lbo", // system.r_ud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6InN5c3RlbS5yd19kQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5NjYzNCwiZXhwIjoxNjMzMDg4NjM0LCJhaWwiOltdLCJsdmwiOjEzOX0.9lXAjUsjq4hhayj5NHDfOqkdadUxabux5YwcHM6wNzo", // system.rw_d@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6InN5c3RlbS5yd3VfQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5NjY2MiwiZXhwIjoxNjMzMDg4NjYyLCJhaWwiOltdLCJsdmwiOjEzNX0.epy8PcJ24W0MYfGKsXi4X40D1LJmv1Rm6QMe5UjXjds", // system.rwu_@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6ImFkbWluaXN0cmF0b3Iucnd1ZEB1c2VyaGFiaXQuaW8iLCJpYXQiOjE2MzA0OTY3MjYsImV4cCI6MTYzMzA4ODcyNiwiYWlsIjpbXSwibHZsIjo3OX0.0evo2xPxrs_aKyPumGZqhV_9W8z-RzGq70KlipodxJo", // administrator.rwud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6ImFkbWluaXN0cmF0b3IuX3d1ZEB1c2VyaGFiaXQuaW8iLCJpYXQiOjE2MzA0OTY3NzAsImV4cCI6MTYzMzA4ODc3MCwiYWlsIjpbXSwibHZsIjo3OH0.wG_7xAdGe1i-dhmqlD9QJg5lvV9g7ZYu9n8I-nSaOxY", // administrator._wud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6ImFkbWluaXN0cmF0b3Iucl91ZEB1c2VyaGFiaXQuaW8iLCJpYXQiOjE2MzA0OTY3OTgsImV4cCI6MTYzMzA4ODc5OCwiYWlsIjpbXSwibHZsIjo3N30.p2KrlJ79MFwLNHrWgyhEkhECmZHpiyNjY3Mrpb_7HoA", // administrator.r_ud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6ImFkbWluaXN0cmF0b3IucndfZEB1c2VyaGFiaXQuaW8iLCJpYXQiOjE2MzA0OTY4MjQsImV4cCI6MTYzMzA4ODgyNCwiYWlsIjpbXSwibHZsIjo3NX0.wk-JbuJrqbJ0hhzSNMPlWou9qHmBLH4aAUHdP9ymVb0", //administrator.rw_d@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6ImFkbWluaXN0cmF0b3Iucnd1X0B1c2VyaGFiaXQuaW8iLCJpYXQiOjE2MzA0OTY4NDQsImV4cCI6MTYzMzA4ODg0NCwiYWlsIjpbXSwibHZsIjo3MX0.QoRxhJZD4uelEGkrPkRTi1DoDsrp6lAa7yqG5Tybfg4", //administrator.rwu_@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6Im1lbWJlci5yd3VkQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5Njg3MCwiZXhwIjoxNjMzMDg4ODcwLCJhaWwiOltdLCJsdmwiOjMxfQ.cWVjQvRJh18xHh0TA1tMY6Xv2rBYeYM9BrLnFg9AakU", //member.rwud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6Im1lbWJlci5fd3VkQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5NjkyMywiZXhwIjoxNjMzMDg4OTIzLCJhaWwiOltdLCJsdmwiOjMwfQ.UM_63EdL3aOZgRnUv_03cQS9XP_3rFi3I8V4Vaadtow", //member._wud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6Im1lbWJlci5yX3VkQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5Njk0NywiZXhwIjoxNjMzMDg4OTQ3LCJhaWwiOltdLCJsdmwiOjI5fQ.b6T_PMGAmj5VUoA_C5Wbh8v6Ns42j8J7O152EJxYrZQ",//member.r_ud@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6Im1lbWJlci5yd19kQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5Njk2NSwiZXhwIjoxNjMzMDg4OTY1LCJhaWwiOltdLCJsdmwiOjI3fQ.3RVCcxMRu1iHfrUXqO4UpQADydGa8ynSZ-TME1qJ6zE", //member.rw_d@userhabit.io
			// "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6Im1lbWJlci5yd3VfQHVzZXJoYWJpdC5pbyIsImlhdCI6MTYzMDQ5Njk4NiwiZXhwIjoxNjMzMDg4OTg2LCJhaWwiOltdLCJsdmwiOjIzfQ.Yn7_EClEzOPKzXexMwDptZsh7NNqDdS_tFxdFIzSEk0",//member.rwu_@userhabit.io
			"id": "test01@userhabit.io",
			"password": "abcd1234",
			"from_date": "2021-01-17T00:00:00Z",
			"to_date": "2021-12-31T00:00:00Z",
			"app_id_list": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
			"session_id_list": "2_187c43ad3e07413395e622d2a0cfc49c",
			"crash_id_list":"-1534139170",
			"_": ""
		}
	}
}
```
- src/main/resources/test/rest_client_test/*.http 파일 아무거나 하나 열기
- Send Request 버튼 클릭

## 테스트 계정에 레벨값 가져오는 샘플 코드
```kotlin
listOf(
	Level.createLevel(Level.SYSTEM, Level.READ, Level.WRITE, Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.SYSTEM, /*Level.READ,*/ Level.WRITE, Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.SYSTEM, Level.READ, /*Level.WRITE, */Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.SYSTEM, Level.READ, Level.WRITE, /*Level.MODIFY, */Level.DELETE),
	Level.createLevel(Level.SYSTEM, Level.READ, Level.WRITE, Level.MODIFY, /*Level.DELETE*/),

	Level.createLevel(Level.ADMINISTRATOR, Level.READ, Level.WRITE, Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.ADMINISTRATOR, /*Level.READ,*/ Level.WRITE, Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.ADMINISTRATOR, Level.READ, /*Level.WRITE,*/ Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.ADMINISTRATOR, Level.READ, Level.WRITE, /*Level.MODIFY,*/ Level.DELETE),
	Level.createLevel(Level.ADMINISTRATOR, Level.READ, Level.WRITE, Level.MODIFY, /*Level.DELETE*/),

	Level.createLevel(Level.MEMBER, Level.READ, Level.WRITE, Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.MEMBER, /*Level.READ,*/ Level.WRITE, Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.MEMBER, Level.READ, /*Level.WRITE,*/ Level.MODIFY, Level.DELETE),
	Level.createLevel(Level.MEMBER, Level.READ, Level.WRITE, /*Level.MODIFY,*/ Level.DELETE),
	Level.createLevel(Level.MEMBER, Level.READ, Level.WRITE, Level.MODIFY, /*Level.DELETE*/),
	)
```