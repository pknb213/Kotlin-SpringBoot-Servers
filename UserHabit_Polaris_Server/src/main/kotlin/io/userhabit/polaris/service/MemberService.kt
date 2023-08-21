package io.userhabit.polaris.service

import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.userhabit.common.*
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P

/**
 * 로그인, 회원가입, 회원탈퇴, 패스워드 변경, 이메일 인증, 인증 이메일 발송
 *
 * @author nsb
 */
object MemberService {
	private val log = Loggers.getLogger(this.javaClass)
	val COLLECTION_NAME = "member"
	// @deprecated
	// lateinit var FIELD_LIST: List<Map<String, String>>

	/**
	 * 회원 목록
	 * @comment 21.11.18 yj
	 * @sampleGET {{localhost}}/v3/member/{ids}
	 * @param
	 * 		by: '', "date"
	 * @return
		 * data = [
			 * {
				 * "name": "이름(테스터01)",
				 * "created_date": "2021-05-01T12:00:00Z",
				 * "_id": "000000000000000000000101"
			 * },...
		 * ]
	 */
	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val ids = req.splitParamOrDefault("ids","")

		val date = req.getQueryOrDefault("date", "")
		val sortField = req.getQueryOrDefault("sort_field", "created_date")
		val sortValue = req.getQueryOrDefault("sort_value", -1)
		val skip = req.getQueryOrDefault("skip",0)
		val limit = req.getQueryOrDefault("limit", 50)
		val fieldList = req.splitQueryOrDefault("field_list", "name,created_date")
		val duration = req.getQueryOrDefault("duration", "day")
		val by = req.getQueryOrDefault("by", )
		val searchExpr = req.getQueryOrDefault("search_expr",)

		val vali = Validator()
			.new(limit, "limit").max(100)

		if (by == "date")
			vali.new(date, "date").required().date()

		if(vali.isNotValid()){
			return Mono.just(Status.status400BadRequest(Message.get(req.lang, Message.badRequest), vali.toExceptionList()))
		}

		if(by == "date") {
			val dateKey = if(by == "login") "\$${P.ldK}" else "\$${P.cdK}"
			val groupDuration = if (duration == "month") {
				D("\$dateToString", D("format", "%Y-%m-01T00:00:00.000Z").append("date", dateKey))
			} else if (duration == "week") {
				D("\$toString", D("\$dateFromString", D("format", "%G-%V").append("dateString", D("\$dateToString", D("format", "%G-%V").append("date", dateKey)))))
			} else if (duration == "day") {
				D("\$dateToString", D("format", "%Y-%m-%dT00:00:00.000Z").append("date", dateKey))
			} else if (duration == "hour") {
				D("\$dateToString", D("format", "%Y-%m-%dT%H:00:00.000Z").append("date", dateKey))
			} else if (duration == "year") {
				D("\$dateToString", D("format", "%Y-01-01T00:00:00.000Z").append("date", dateKey))
			} else { // all
				D()
			}

			val matchQuery = D().append(P.cdK, D("\$eq", Date.from(Instant.parse(date))))

			val projectDoc = D().append("_id", 0)
				.append(P.cdK,"\$_id.${P.cdK}")
				.append(P.ldK,"\$_id.${P.ldK}")
				.append(P.uiK,"\$_id.${P.ui}")
				.append(P.unK,"\$_id.${P.un}")
				.append(P.ucK,"\$_id.${P.uc}")

			val query = listOf(
				D("\$match", matchQuery),
				D("\$group", D()
					.append("_id", D()
						.append(P.cdK, "\$${P.cdK}") //app list 에서 필요
						.append(P.ldK, groupDuration)
						.append(P.ui,"\$_id")
						.append(P.un,"\$${P.unK}")
						.append(P.uc,"\$${P.ucK}")
					)),
				D("\$project", projectDoc),
				D("\$sort", D(sortField, sortValue)),
				D("\$skip", skip),
				D("\$limit", limit)
			)
			log.debug(query.map { Util.toPretty(it) }.toString())
//			println(Util.toPretty(query))
			return Flux
				.from(MongodbUtil.getCollection(COLLECTION_NAME)
					.aggregate(query)
				)
				.collectList()
				.map { Status.status200Ok(it) }
		} else {
			return Flux
				.from(
					MongodbUtil.getCollection(COLLECTION_NAME)
						.find( D().let{
							if(searchExpr.isNotEmpty()) it.putAll(D.parse(searchExpr))// 이 로직은 반드시 아래 조건 추가 로직보다 우선되어야 함

							val level = req.getLevel()
							if(Level.hasPermission(level, Level.SYSTEM)) {
								if(ids.first() != "*") it.append("_id", D("\$in", ids.map { ObjectId(it) }) )
							}else{
								it.append("company_id", req.getCompanyID())
								if(Level.hasPermission(level, Level.ADMINISTRATOR) || Level.hasPermission(level, Level.MANAGER)){
									if(ids.first() != "*") it.append("_id", D("\$in", ids.map { ObjectId(it) }) )
								}else{
									it.append("email", req.getUserId())
								}
							}

							it
						} )
						.sort(D(sortField, sortValue))
						.skip(skip)
						.limit(limit)
						.projection(
							fieldList.fold(D()){ acc, it -> acc.append(it, 1)}
								.let {
									it.append("_id", D("\$toString", "\$_id"))
									if(it.containsKey("email_auth_date")) it.append("email_auth_date", D("\$dateToString", D("date", "\$email_auth_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
									if(it.containsKey("login_date")) it.append("login_date", D("\$dateToString", D("date", "\$login_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
									if(it.containsKey("created_date")) it.append("created_date", D("\$dateToString", D("date", "\$created_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
									if(it.containsKey("updated_date")) it.append("updated_date", D("\$dateToString", D("date", "\$updated_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
									// 위험 데이터는 절대 노출 금지
									it.remove("password")
									it.remove("secret_key")
									it
								}
						)
				)
				.collectList()
				.map {
					Status.status200Ok(it)
				}
		}
	} //  end of get()

	/**
	 * 회원가입
	 * 로직이 복잡해서 post, put 분리
	 * @comment 21.11.18 yj
	 * @sample POST {{localhost}}/v3/member
	 * @param content-type: text/plain
		 * [{
			"email": "WooYoungMi@expensive.io",
			"password": "abcd1234",
			"name": "영미"
		 * }]
	 * @return data: [...]
	 * {
		"modifiedCount": 0,
		"deletedCount": 0,
		"insertedCount": 0,
		"matchedCount": 0,
		"upserts": [
			{
				"index": 0,
				"id": {
					"value": {
						"timestamp": 1636618160,
						"date": 1636618160000
					},
				"bsonType": "OBJECT_ID",
				"array": false,
				"null": false,
				"decimal128": false,
				"timestamp": false,
				"dbpointer": false,
				"regularExpression": false,
				"javaScript": false,
				"javaScriptWithScope": false,
				"document": false,
				"int32": false,
				"int64": false,
				"string": false,
				"dateTime": false,
				"objectId": true,
				"symbol": false,
				"binary": false,
				"number": false,
				"double": false,
				"boolean": false
				}
			}
		],
		"inserts": []
	 * }
	 * @throws 관리자 확인을 Email Object에서 하는데 it["id"]가 없어 에러 발생
	 */
	fun post(req: SafeRequest): Mono<Map<String,Any>> {
		val bodyList =  try { req.getBodyJsonToList() }catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}

		val vali = Validator()
		bodyList.forEach {
			vali.new(it, "email").required().email()
			vali.new(it, "password").required().password()
			vali.new(it, "name").required()
		}
		vali.new(bodyList, "body").required()

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		val modelList = bodyList.map { body ->
			val updateDoc = D()
				.append("active_status", true)
				.append("secret_key", UUID.randomUUID().toString())
				.append("email_auth_key", null)
				.append("login_date", null)
				.append("created_date", Date())
				.append("updated_date", Date())
				.append("expiry_date", null)
				.append("withdraw_msg", null)
				.append("login_count", 0)
				.append("job_type", 0)
				.append("locale", null) // TODO 앱 콜랙션에 있어야 되지 않나?
				.append("telephone", null)
				.append("level", Level.createLevel(Level.MEMBER, Level.READ)) // 가입시는 멤버에 읽기만 가능
				.append("email", body["email"])
				.append("password", Util.sha256Base64(body["password"].toString()))
				.append("name", body["name"])
				.append("telephone", body["telephone"])

			UpdateOneModel<D>(
				D("_id", ObjectId() ),
				D("\$set", updateDoc),
				UpdateOptions().upsert(true))
		}

		return Mono
			.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(modelList))
			.map {
				// TODO 관리자인지 내부 서비스에서 호출한건지 검사
				Flux
					.fromIterable(bodyList)
					.flatMap {
						val id = it["id"] as String
						Email
							.sendAuthentication(req.lang, id, it["name"] as String)
							.flatMap {
								val error = it["error"]
								val authKey = it["auth_key"]!!
								if (error == null) { // 에러가 없으면
									Mono.from(
										MongodbUtil.getCollection(COLLECTION_NAME)
											.updateOne(D("_id", id), D("\$set", D(P.eakK, authKey)))
									)
								} else {
									// user 컬랙션에 실패했다고 기록할까?? email 컬랙션에 이미 기록 하는데...고민...
									Mono.error(Exception(error.toString()))
								}
							}
					}
					.subscribe() // 중요!! 이 로직에서는 비동기로 호출 함. rx스트림 연결하면 하나하나 딜레이가 심함
				Status.status200Ok(it, message = "입력되었습니다") // TODO i18n
			}
	} // end of post()

	/**
	 * 회원수정
	 * 로직이 복잡해서 post, put 분리
	 *
	 * 기존 비밀번호 알고 있는 상태  = password_origin 필수
	 * or
	 * 비밀번호 잊어버려 변경할 때 = email_auth_key 필수
	 */
	fun put(req: SafeRequest): Mono<Map<String,Any>> {
		val bodyList =  try { req.getBodyJsonToList() }catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}
//		val isSystem = Level.contains(req.getLevel(), Level.SYSTEM)
		val isSystem = false
		val loggedEmail = req.getUserId()
		val level = req.getLevel()

		val vali = Validator()
		bodyList.forEach {
			vali.new(it, "id").required()
			vali.new(it, "email").email().required()
			if(!Level.contains(level, Level.SYSTEM, Level.ADMINISTRATOR)){
				vali.new(it, "level").ifExistDenied()
			}
			vali.new(it, "level").hasSetPermission(level)

			val passwordOrigin = it["password_origin"]
			val passwordNew = it["password_new"]
			val passwordConfirm = it["password_confirm"]
			if (passwordOrigin != null || passwordNew != null || passwordConfirm != null) { // 값이 있다는건 비번을 바꾸겠단 의도
				vali.new(passwordNew, "password_new").required().password()
				vali.new(passwordConfirm, "password_confirm").required()
				vali.new(passwordNew == passwordConfirm, "password_new == password_confirm").must(true)
				if(!isSystem){
					vali.new(passwordOrigin, "password_origin").required().password()
					vali.new((passwordOrigin as? String ?: "") + (it["email_auth_key"] as? String ?: ""), "password_origin or email_auth_key").required()
					vali.new(loggedEmail == it["email"], "email").custom(true, "It must be the same logged in email and requested email")
				}
			}

			if(it["active_status"] == false){ // 멤버 탈퇴
				vali.new(it, "admin_email").email()
				vali.new(it, "withdraw_msg").required()
				//vali.new((it["withdraw_msg"] as? String ?: "" )+(it["admin_email"] as? String ?: "" ), "withdraw_msg or admin_email").required()
			}
		}
		vali.new(bodyList, "[POST or PUT] body").required()

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		val valiInner = Validator()

		return Mono
			.zip(
				let { // 시스템이 아니면 로그인한 이메일과 같은지 검사
					if(isSystem) Mono.just(true)
					else Flux
						.from(
							MongodbUtil.getCollection(COLLECTION_NAME)
								.find(D("_id", D("\$in", bodyList.map { ObjectId(it["id"] as String) })))
								.projection(D().append("email", 1))
						)
						.collectList()
						.map { docList ->
							bodyList.forEach { row ->
								docList.find { loggedEmail == it["email"] }
									?: valiInner.new(false, "email").custom(true, "It must be the same logged in email and requested email")
							}
						}
				},
				bodyList // 비밀번호 변경 검사
					.filter { it.containsKey("password_new")}
					.let { filteredList ->
						if(filteredList.isEmpty()) Mono.just(true)
						else Flux
							.from(
								MongodbUtil.getCollection(COLLECTION_NAME)
									.find(D("_id", D("\$in", filteredList.map { ObjectId(it["id"] as String) })))
									.projection(D("_id", D("\$toString", "\$_id")).append("password", 1).append("email_auth_key", 1).append("level", 1).append("email", 1))
							)
//							.switchIfEmpty {
//								it.onError(PolarisException.status400BadRequest(listOf(mapOf("ID" to "not found"))))
//							}
							.collectList()
							.map { docList ->
								// 비밀번호 변경이 있으면 비밀번호 맞는지 확인

								if(!isSystem){
//								val docMap = docList.fold(mutableMapOf<String, D>()) { acc, doc -> acc.apply { put(doc["_id"].toString(), doc) } }
									filteredList.forEach { row ->
//										val doc = docMap[it["id"]]!!
										val doc = docList.find { row["id"] == it.getString("_id") }!!

										if(Util.sha256Base64(row["password_origin"] as? String ?: "") != doc["password"])
											valiInner.new(false, "password_origin").custom(true, "is invalid")

										if(doc["password"] != Util.sha256Base64(row["password_origin"] as? String ?: "" )
											&& doc["email_auth_key"] != row["email_auth_key"] )
											valiInner.new(null, "email_auth_key or password_origin").custom(true, "It must be a valid email_auth_key or password")
									}
								}
							}
					}, // end of arg1
				bodyList // 회원탈퇴 검사
					.filter { it["active_status"] == false && it.containsKey("admin_email")}
					.let { filteredList ->
						if(filteredList.isEmpty()) Mono.just(true)
						else Flux
							.from(
								MongodbUtil.getCollection(COLLECTION_NAME)
									.find( D("email", D("\$in", filteredList.map {it["admin_email"]})) .append("level", D("\$bitsAllSet", Level.ADMINISTRATOR)) )
									.projection(D("_id", 1).append("level", 1).append("email", 1))
							)
							.collectList()
							.map { docList ->
								var isValid = true
								filteredList.forEach { row ->
									docList.find { row["admin_email"] == it["email"] }
										?: let{
											isValid = false
											valiInner.new(false, "email").custom(true, "not found [${row["admin_email"]}] or not an admin")
										}
								}

								if(isValid) Mono
									.from(
										MongodbUtil.getCollection(COLLECTION_NAME).updateMany(
											D("_id", D("\$in", docList.map { it["_id"] })),
											D("\$set", D("level", Level.createLevel(Level.ADMINISTRATOR, Level.READ, Level.WRITE, Level.UPDATE, Level.DELETE)) )
										)
									)
									.subscribe()
							}
					} // end of arg2
			)
			.flatMap {
				""" Todo : 로직 변경 점: password_new가 먼저 password 값에 치환되고나서 
					Init password 값에 덮어써져 변경이 안되는 버그가 있음. 
					secret_key 값 또한 마찬가지
				""".trimMargin()

				if (valiInner.isNotValid()) throw PolarisException.status400BadRequest(vali.toExceptionList())

				val modelList = bodyList.map { body ->
					val updateDoc = D()
					body["email"        ]?.let{ updateDoc.append("email"         , it)}
					body["locale"       ]?.let{ updateDoc.append("locale"        , it)}
					body["password"     ]?.let{ updateDoc.append("password"      , Util.sha256Base64(it.toString()))}
					body["name"         ]?.let{ updateDoc.append("name"          , it)}
					body["telephone"    ]?.let{ updateDoc.append("telephone"     , it)}
					body["job_type"     ]?.let{ updateDoc.append("job_type"      , it)}
					body["level"        ]?.let{ updateDoc.append("level"         , it)}
					body["withdraw_msg" ]?.let{ updateDoc.append("withdraw_msg"  , it); updateDoc.append("withdraw_date", Date())}
					body["active_status"]?.let{
						updateDoc.append("active_status" , it)
						if(it == false) updateDoc.append("secret_key", UUID.randomUUID().toString())
					}
					(body["password_new" ] ?: body["password"])?.let{
						updateDoc.append("password", Util.sha256Base64(it as String))
						updateDoc.append("secret_key", UUID.randomUUID().toString())
					}
					updateDoc.append("updated_date"   , Date())
//					TODO add, check field

					UpdateOneModel<D>(
						D("_id", ObjectId(body["id"] as String) ),
						D("\$set", updateDoc),
						UpdateOptions().upsert(true))
				}

				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(modelList))
			}
			.map {
				bodyList
					.filter { it["active_status"] == false || it["password_new"] != null}
					.forEach {
						lruCache.remove(it["email"]) // 다시 캐쉬 저장
					}

				Status.status200Ok(it, message = "입력되었습니다") // TODO i18n
			}
	} // end of put()

	/**
	 * 회원탈퇴
	 */
	fun delete(req: SafeRequest): Mono<Map<String, Any>> {
		val userIdList = req.splitParamOrDefault("ids")

		val vali = Validator()
		userIdList.forEach {
			vali.new(it, "id").required().email()
		}
		vali.new(userIdList, "ids").required()

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Mono
			.zip(
				// 유료 플랜이 있는 경우 Administrator 계정은 삭제 할 수 없다. (플랜 삭제 후 계정 삭제)
				// zip1
				if(Level.hasPermission(req.getLevel(), Level.ADMINISTRATOR)) Flux // TODO Level.MANAGER 도 해야 하나?
					.from(
						MongodbUtil.getCollection(AppService.COLLECTION_NAME)
							.find(D("member_id", D("\$in", userIdList)))
							.projection(D("_id", 1).append("plan_id", 1))
					)
					.switchIfEmpty {
						it.onError(PolarisException.status400BadRequest(mapOf("ids" to "not found")))
					}
					.filter {
						"enterprise business startup".contains(it["plan_id"].toString())
					}
					.collectList()
					.map { docList ->
						// 유료 플랜이 하나라도 있으면
						if(docList.size > 0){
							val ids = docList.map { mapOf("app_id" to "'${it.getString("_id") }' must delete the plan before delete users")}
//							throw PolarisException.status400BadRequest(ids)
						}
					}
				else
					Mono.just(1)
				,
				//zip2
				Flux
					.from(
						MongodbUtil.getCollection(COLLECTION_NAME)
							.find(D("_id", D("\$in", userIdList)))
							.projection(D("_id", 1).append("level", 1))
					)
					.collectList()
					.map { docList ->
						val ids = userIdList.filter { id -> docList.find { it.getString("_id") == id } == null }
						// 파라미터 아이디값이 디비에 하나라도 없으면
						if(ids.size > 0) throw PolarisException.status400BadRequest(ids.map {
							mapOf("id[${it}]" to "not found")
						})

						val ids2 = docList.filter {
							req.getUserId() != it.getString("_id") && !Level.hasDeletePermission(req.getLevel(), it.getLong("level"))
						}
						// 권한 없는 아이디가 하나라도 있으면
						if(ids2.size > 0) throw PolarisException.status403Forbidden(ids2.map {
							mapOf("id[${it}]" to "")
						})
					}
			)
			.flatMap {
				Mono.from(
					MongodbUtil.getCollection(COLLECTION_NAME).updateMany(
						D("_id", D("\$in", userIdList)),
						D("\$set", D("expiry_date", LocalDateTime.now().plusYears(1))
							.append("updated_date", Date()))
					)
				)
			}
			.map {
				// TODO delete
//				if(it.matchedCount < 1)
//					throw PolarisException.status400BadRequest()

				Status.status200Ok(it)
			}
	} // end of delete()

	/**
	 * @comment 21.11.18 yj
	 * @sample POST {{localhost}}/v3/member/login
	 * @param content-type: application/json
		 * {
			"email":"WooYoungMi@expensive.io",
			"password":"abcd1234"
		 * }
	 * @return
	 * {
		"status": 200,
		"message": "Has been logged",
		"data": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6Ildvb1lvdW5nTWlAZXhwZW5zaXZlLmlvIiwiaWF0IjoxNjM2NjUxNjM1LCJleHAiOjE2MzkyNDM2MzUsImFpbCI6bnVsbCwibHZsIjoxNywiY2lkIjpudWxsfQ.y2RuBxFkgmTHO5pa-J2bKCnEyJP0XkgQlHU7-NKGRx4",
		"field_list": []
	 * }
	 * @see email_auth_date 필수, null인 경우 401 ERROR
	 */
	fun login(req: SafeRequest): Mono<Map<String, Any>> {
		val email = req.getBodyOrDefault("email", "")
		val password = req.getBodyOrDefault("password", "")

		val vali = Validator()
			.new(email, "email").required().email()
			.new(password, "password").required().password()

		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Mono
			.from(MongodbUtil.getCollection(COLLECTION_NAME).aggregate(
				listOf(
					D("\$match", D("email", email)),
					D("\$lookup", D()
						.append("from", "app")
						.append("localField", "company_id")
						.append("foreignField", "company_id")
						.append("as", "app_list")
					),
					D("\$project", D()
						.append("_id", 0)
						.append("email", 1)
						.append("email_auth_date", 1)
						.append("password", 1)
						.append("secret_key", 1)
						.append("level", 1)
						.append("company_id", 1)
						.append("active_status", 1)
						.append("login_count", 1)
						.append("app_id_list", "\$app_list._id")
					),
				)
			))
			.defaultIfEmpty(D())
			.flatMap { doc ->
				if (doc.isEmpty()) {
					throw PolarisException.status400BadRequest(listOf(mapOf("email" to "Email not found")))
				} else if (doc["email_auth_date"] == null) {
					throw PolarisException.status401Unauthorized()
				} else if (doc["active_status"] == null ||  doc["active_status"] == false) {
					throw PolarisException.status400BadRequest(listOf(mapOf("email" to "Email has been withdrawn")))
				} else if (doc["password"] != Util.sha256Base64(password)) {
					throw PolarisException.status400BadRequest(listOf(mapOf("password" to "Password is invalid")))
				}

				Mono
					.from(
						MongodbUtil.getCollection(COLLECTION_NAME)
							.updateOne(D("email", email),
								D("\$set", D("login_date", Date()))
									.append("\$inc", D("login_count", 1))
							)
					)
					.zipWith(
						LoginHistoryService.post(req)
					)
					.map {
						val ldt = LocalDateTime.now()
						val jwt = Util.encodeJWT(
							Util.toJsonString(mapOf(
								"id" to doc["email"],
								"iat" to ldt.toEpochSecond(ZoneOffset.UTC),
								"exp" to ldt.plusDays(30).toEpochSecond(ZoneOffset.UTC),
								"ail" to doc["app_id_list"],
								"lvl" to doc["level"],
								"cid" to doc["company_id"].toString(),
							)),
							doc["secret_key"] as String)
						Status.status200Ok(jwt, listOf(), "Has been logged")
					}
			}
	} // end of login()

	fun authEmail(req: SafeRequest): Mono<Map<String, Any>> {
		val email = req.getBodyOrDefault("email","")
		val key = req.getBodyOrDefault("key","")

		val vali = Validator()
			.new(email, "email").required().email()
			.new(key, "key").required()

		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Mono
			.from(MongodbUtil.getCollection(COLLECTION_NAME).updateOne(
				D("email", email),
				D("\$set", D("email_auth_date", Date())
					.append("email_auth_key", key)
				)
			))
			.map {
				if(it.matchedCount < 1) throw PolarisException.status401Unauthorized("Invalid key or id")
				else Status.status200Ok(it)
			}
	}

	fun invite(req: SafeRequest): Mono<Map<String,Any>> {
		val level = req.getLevel()
		if(!Level.contains(level, Level.SYSTEM, Level.ADMINISTRATOR))
			throw PolarisException.status403Forbidden()

		val bodyList =  try { req.getBodyJsonToList() }catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}
			.map {
				it.plus("authKey" to UUID.randomUUID().toString())
			}

		val vali = Validator()
		bodyList.forEach {
			vali.new(it, "email").required().email()
			vali.new((it["level"] as? Number)?.toLong() ?: 0L, "level").required(listOf(Level.MANAGER, Level.MEMBER))
		}
		vali.new(bodyList, "post body").required()
		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		val coll = MongodbUtil.getCollection(COLLECTION_NAME)
		return Mono
			.from(
				coll.find(D("email", req.getUserId())).projection(D("name", 1).append("company_id", 1))
			)
			.zipWhen { doc ->
				val now = LocalDateTime.now()
				Mono.from(
					coll
						.insertMany( bodyList.map {
							D()
								.append("email" , it["email"])
								.append("company_id", doc["company_id"])
								.append("email_auth_key",  it["authKey"])
								.append("active_status",  it["active_status"])
								.append("level",  Level.createLevel(Level.MEMBER, Level.READ))
								.append("created_date",  now)
								.append("updated_date",  now)
								.append("expiry_date",  now.plusMonths(3)) // TODO 몇개월로 > 보름님
								.append("login_count",  0)
						})
				)
			}
			.map { tuple ->
				Flux
					.fromIterable(bodyList)
					.flatMap {
						Email.sendInviteMember(req.lang, it["authKey"].toString(),
							req.getUserId(), tuple.t1["name"].toString(),
							it["email"].toString(), it["name"]?.toString() ?: it["email"].toString())
					}
					.subscribe() // 중요!! 메일은 처리가 늦기 때문에 비동기로 실행해야 함. 전송 여부는 따로 이메일 컬랙션 참조

				Status.status200Ok(tuple.t2)
			}
	} // end of invite()

	// id별로 secret_key 캐시를 위해
	private val lruCache = object: java.util.LinkedHashMap<String, String>(){
		override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean {
			return if(super.size > 500) true else false
		}
	}

	fun getSecretKey(id: String): Mono<String> {
		if(id.isEmpty()) return Mono.just("")
		return lruCache[id]
			.let {
				if(it != null)
					Mono.just(it)
				else
					Mono
						.from(
							MongodbUtil.getCollection(COLLECTION_NAME)
								.find(D("email", id))
								.projection(D("secret_key", 1))
						)
						.defaultIfEmpty(D())
						.map{
							if(it.isEmpty()) throw PolarisException.status401Unauthorized()
							lruCache.put(id, it["secret_key"] as String)
							it["secret_key"] as String
						}
			}
	}

	fun removeSecretKeyCache(id: String) {
		lruCache.remove(id)
	}

	/**
	 * @author jelee
	 * @comment 21.11.18 yj
	 * @sample GET {{localhost}}/v3/member/{ids}/count?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z&by=accumulated
	 * @return data: [...]
	 * {
		"created_date": "2021-05-01T00:00:00Z",
		"user_count": 19
	 * } ...
	 * @see by 기본 값이 "" 없어서 주어야한다. 아래 todo 참조
	 */
	fun count(req: SafeRequest): Mono<Map<String, Any>> {
		val versionList = req.splitQueryOrDefault("version_list", "")
		val fromDate = req.getQueryOrDefault("from_date", "")
		val toDate = req.getQueryOrDefault("to_date", "")
		val sortField = req.getQueryOrDefault("sort_field", "_id")
		val sortValue = req.getQueryOrDefault("sort_value", 1)
		val skip = req.getQueryOrDefault("skip", 0)
		val limit = req.getQueryOrDefault("limit", 40)
		val by = req.getQueryOrDefault("by", "")
		val duration = req.getQueryOrDefault("duration", "day")
//		val by = req.getQueryOrDefault("by", "session")
		val type = req.getQueryOrDefault("type", "base")
//		val isUnique = req.getQueryOrDefault("is_unique", "false").toBoolean()
		val coll: String
		val query: List<D>

		val vali = Validator()
//			.new(appIdList, "app_id_list").required()
			.new(fromDate, "from_date").required().date()
			.new(toDate, "to_date").required().date()
			.new(duration, "duration").required(listOf("all", "day", "week", "month", "year"))
//			.new(type, "type").required(listOf("all", "no_response", "swipe", "long_tap", "double_tap", "tap", "swipe_direction"))
//		.new(by, "by").required(listOf("all" ,"view" ))
			.new(limit, "limit").max(100)

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		val dateKey = if(by == "login") "\$${P.ldK}" else "\$${P.cdK}"
		val groupDuration = if (duration == "month") {
			D("\$dateToString", D("format", "%Y-%m-01T00:00:00.000Z").append("date", dateKey))
		} else if (duration == "week") {
			D("\$toString", D("\$dateFromString", D("format", "%G-%V").append("dateString", D("\$dateToString", D("format", "%G-%V").append("date", dateKey)))))
		} else if (duration == "day") {
			D("\$dateToString", D("format", "%Y-%m-%dT00:00:00.000Z").append("date", dateKey))
		} else if (duration == "hour") {
			D("\$dateToString", D("format", "%Y-%m-%dT%H:00:00.000Z").append("date", dateKey))
		} else if (duration == "year") {
			D("\$dateToString", D("format", "%Y-01-01T00:00:00.000Z").append("date", dateKey))
		} else { // all
			D()
		}

		if(by == "new" || by =="accumulated"){
			val projectDoc = D().append("_id", 0)
				.append(P.cdK,"\$_id.${P.cdK}")
				.append(P.ucoK, "\$${P.uco}")

			val matchQuery = if (by =="accumulated") {
				D().append(P.cdK, D("\$lte", Date.from(Instant.parse(fromDate))))
			} else {
				D().append(P.cdK, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))))
			}

			query = listOf(
				D("\$match", matchQuery),
				D("\$group", D()
					.append("_id", D()
						.append(P.cdK, "\$${P.cdK}") //app list 에서 필요
						.append(P.cdK, groupDuration)
					)
					.append(P.uco, D("\$sum", 1))),
				D("\$project", projectDoc),
				D("\$sort", D(sortField, sortValue)),
				D("\$skip", skip),
				D("\$limit", limit)
			)
			log.debug(query.map { Util.toPretty(it) }.toString())

			return Flux
				.from(MongodbUtil.getCollection(COLLECTION_NAME)
					.aggregate(query)
				)
				.collectList()
				.map { Status.status200Ok(it) }
		} else if (by == "login") {
			val projectDoc = D().append("_id", 0)
				.append(P.ldK,"\$_id.${P.ldK}")
				.append(P.lcoK,"\$${P.lcoK}")
				.append(P.lacoK,"\$${P.lacoK}")

			val matchQuery = D().append(P.ldK, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))))
			query = listOf(
				D("\$match", matchQuery),
				D("\$group", D()
					.append("_id", D()
						.append(P.ldK, groupDuration)
					)
					.append(P.lcoK, D("\$sum", "\$${P.lcoK}"))
					.append(P.lacoK, D("\$sum", 1))
				),
				D("\$project", projectDoc),
				D("\$sort", D(P.ldK, 1)),
				D("\$limit", limit)
			)
			log.debug(query.map { Util.toPretty(it) }.toString())

			return Flux
				.from(MongodbUtil.getCollection(COLLECTION_NAME)
					.aggregate(query)
				)
				.collectList()
				.map { Status.status200Ok(it) }
		} else if (by == "adIndicators") {
			val projectDoc = D().append("_id", 0)
				.append(P.cdK,"\$_id.${P.cdK}")
				.append(P.accK,"\$${P.accK}")
				.append(P.accmK,"\$${P.accmK}")

			val matchQuery = D().append(P.cdK, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))))
			query = listOf(
				D("\$match", matchQuery),
				D("\$group", D()
					.append("_id", D()
						.append(P.cdK, groupDuration)
					)
					.append(P.accK, D("\$sum", 1))
				),
				D("\$project", projectDoc),
				D("\$sort", D(P.cdK, 1)),
				D("\$limit", limit)
			)
			log.debug(query.map { Util.toPretty(it) }.toString())

			return Flux
				.from(MongodbUtil.getCollection(COLLECTION_NAME)
					.aggregate(query)
				)
				.collectList()
				.map { Status.status200Ok(it) }
		} else{
			return Mono.just(mapOf("" to ""))
		}
	}
}
