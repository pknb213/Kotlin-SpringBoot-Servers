package io.userhabit.anejo.service

import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.userhabit.common.MongodbUtil
import io.userhabit.common.SafeRequest
import io.userhabit.common.Status
import io.userhabit.polaris.Protocol as P
import io.userhabit.polaris.service.AppService
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import reactor.util.Loggers
import org.bson.Document as D

/**
 * @author sbnoh
 * @see collection of anejo project 'io/userhabit/anejo/component/request/SessionCreateRequestHandler.java'
 *
 */
object OpenService {
	private val log = Loggers.getLogger(this.javaClass)

	fun post(req: SafeRequest, issuedAt: Long): Mono<Map<String, Any>> {
		// 아네호 api 호출한 response 값
		//{"transportConst":{"t":0,"v":0},"syncTime":1625450007521,"serverTime":1625450007522,"message":"Welcome to DevMode","transportMethod":{"d":"oxo"},"status":"OK"}

		val emptyD = D()
		val body = req.getBodyJsonToDocument()
		// TODO anejo table, logic 분석 필요
//	transportMethodMap.put("d", genConstraint(isDevApp, app.getTransportMethod().intValue(), app.isImgCaptureOn(), isTakeableDevice(devWidth, devHeight)));
		val isDevApp = req.getParamOrDefault("apikey", "").startsWith("dev_")
		val (width, height) = body["d", emptyD]["s", emptyD].let { it["w", 0L].toDouble() to it["h", 0L].toDouble() }
		val isTakeableDevice = (if(width > height) width / height else height / width)
			.let {
				it >= 1.6 && it <= 2;
			}
		val transportMethod = 1
		val isImgCaptureOn = true

		// index[0] = send session data
		// index[1] = collect icon
		// index[2] = img capture
		val constraint = if (isDevApp) {
			if (isImgCaptureOn) "oxo"
			else "oxx"
		} else {
			if (isTakeableDevice) {
				when (transportMethod) {
					1 -> "oxx"
					2 -> "oxx"
					3 -> "wxx"
					else -> ""
				}
			} else {
				when (transportMethod) {
					1 -> "oxx"
					2 -> "oxx"
					3 -> "wxx"
					else -> ""
				}
			}
		}

		val now = System.currentTimeMillis()
		return Mono.just(mapOf(
			"status" to "OK",
			"serverTime" to now,
			"syncTime" to issuedAt / 2 + now / 2, // 각각 반씩 나누고 합치는 이유는 뭘까.... by sbnoh
			"transportConst" to mapOf("t" to 0, "v" to 0), // 하드코딩 0은 뭘까?? by sbnoh
			"transportMethod" to mapOf("d" to constraint),
			"message" to if(isDevApp) "Welcome to DevMode" else "",
		))
	}

	/**
	 * @author san
	 * @param [{ak, si, di..}, {ak, si, di..}, {ak, si, di..}]
	 * response [{ak/si/di true,
	 * 				kill switch,
	 *				config}]
	 * 아래 sessionOpen()함수 참고해서 post() 함수 작성 필요 (확실치 않음 아네호 로직 분석 필요)
	 */
	fun sessionOpen(req: SafeRequest): Mono<Map<String, Any>>{
		val requestBodyList = req.getBodyJsonToList().map{ it.toMutableMap() }
		val appKeyList = requestBodyList.map { it[P.ak] as String }

		return AppService.getAppIdMap(appKeyList).flatMap {
			val resultList = mutableListOf<Map<String, Any>>()

			val mongoUpdateList =
				requestBodyList.filter {requestBody->
					resultList.add(mapOf(
						"AppKey" to requestBody[P.ak] as String,
						"SessionId" to requestBody[P.si] as String,
						"DeviceId" to requestBody[P.di] as String,
						"Authed" to it.containsKey(requestBody[P.ak] as String),
						"KillSwitch" to "please, don't die :(",
						"Config" to "everything"
					))
					it.containsKey(requestBody[P.ak])
				}.map {
					UpdateOneModel<D>(
						D("_id", it.remove(P.si)),
						D("\$set", D(it)),
						UpdateOptions().upsert(true)
					)
				}

			Mono.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).bulkWrite(mongoUpdateList))
				.map {
					Status.status200Ok(resultList)
				}
		}
	}


}
