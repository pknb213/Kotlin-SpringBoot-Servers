package io.userhabit.polaris.service

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.userhabit.common.*
import org.bson.types.ObjectId
import org.bson.Document as D
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.netty.NettyOutbound
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.websocket.WebsocketInbound
import reactor.netty.http.websocket.WebsocketOutbound
import reactor.util.Loggers
import java.nio.charset.Charset
import java.time.Instant
import java.util.*
import kotlin.text.StringBuilder


/**
 * HTTP, WebSocket 서비스들의 공통 처리
 *
 * @author nsb
 */
object ServiceHelper {
	private val log = Loggers.getLogger(this.javaClass)
	private val tempByteBuf = Unpooled.copiedBuffer("".toByteArray())


	/**
	 * 각 서비스에 연결하기 위해 파라미터 생성과 공통의 리스펀스를 처리 한다.
	 */
	fun http(serviceFn: (SafeRequest) -> Mono<Map<String, Any>>, isAuth: Boolean = true): (HttpServerRequest, HttpServerResponse) -> NettyOutbound {
		log.debug("before request-count isAuth: $isAuth")
		val service = if(isAuth){
			{safeReq: SafeRequest ->
				MemberService
					.getSecretKey(safeReq.getUserId())
					.flatMap { secretKey ->
						log.debug("secretKey")
						// TODO 아래 method t검사하는 로직 보다 더 좋은 방법이 없을까??
						if (!Validator.isValidJWT(safeReq.getJWT(), secretKey))
							throw PolarisException.status401Unauthorized()
						else if(safeReq.method() == "GET" && !Level.hasPermission(safeReq.getLevel(), Level.READ))
							throw PolarisException.status403Forbidden(listOf(mapOf("READ" to "Permission denied")))
						else if(safeReq.method() == "POST" && !Level.hasPermission(safeReq.getLevel(), Level.WRITE))
							throw PolarisException.status403Forbidden(listOf(mapOf("WRITE" to "Permission denied")))
						else if(safeReq.method() == "PUT" && !Level.hasPermission(safeReq.getLevel(), Level.UPDATE))
							throw PolarisException.status403Forbidden(listOf(mapOf("UPDATE" to "Permission denied")))
						else if(safeReq.method() == "DELETE" && !Level.hasPermission(safeReq.getLevel(), Level.DELETE))
							throw PolarisException.status403Forbidden(listOf(mapOf("DELETE" to "Permission denied")))

						log.debug("serviceFn(safeReq)")
						serviceFn(safeReq)
					}
			}
		}else{
			serviceFn
		}

		return {req: HttpServerRequest, resp: HttpServerResponse ->
			log.debug("request is received")

			val mono = req.receive().aggregate()
				.defaultIfEmpty(tempByteBuf)
				.flatMap { byteBuf ->
					// TODO SafeRequest 다른 함수에서 실행하면 refCnt = 0 으로 됨.(아마도 쓰레드 관련...) / io.netty.util.IllegalReferenceCountException: refCnt: 0
					val safeReq = SafeRequest(req, byteBuf)
					service(safeReq)
				}
				.onErrorResume {
					if(it is PolarisException || it.cause is PolarisException){
						val ex = if(it.cause is PolarisException ) it.cause as PolarisException else it as PolarisException
						val lang = Locale.LanguageRange.parse(req.requestHeaders()["Accept-Language"] ?: "en").first().range.substring(0, 2)
						println(ex.data)
						Mono.just(
							Status.custom( ex.statusCode, Message.get(lang, ex.messageCode), ex.data )
						)
					}else{
						log.error(it.toString(), it) // TODO 오픈 후에 삭제
						Mono.just(
							Status.status500InteralServerError(it)
						)
					}
				}
				.map {
					val path = req.path()
					if(it["status"] == 200 && path.lastIndexOf("csv") > -1 ){
						val data = it["data"] as List<Map<String,*>>
						// data[0].keys 를 하지 않는 이유 / row 값들중 중간에 없는키가 있을 수 있음
						val keys = data.fold(mutableSetOf<String>()) { acc, it -> acc.addAll(it.keys); acc }

						resp
							.header(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"${path}_${Instant.now()}.csv\"")
							.header(HttpHeaderNames.CONTENT_TYPE, "text/csv")

						val csvString = StringBuilder() // joinToString(",") 대신 성능에 조금이라도 이득을 위해 사용함
						keys.forEach { csvString.append("\"").append(it).append("\",") }
						csvString.deleteAt(csvString.length-1)
						csvString.append("\n")

						data.forEach { row ->
							keys.forEach {
								val value = row[it]
								if(value is String || value is Number || value == null) csvString.append("\"").append(value).append("\",")
								else csvString.append("\"").append(Util.toJsonString(value)).append("\",")
							}
							csvString.deleteAt(csvString.length-1)
							csvString.append("\n")
						}

						// TODO 파라미터로 받아야 하는지 토큰에서 가져와야 하는지 못정함
						val tokenMap = Util.jsonToMap(Util.base64UrlDecoder.decode(req.requestHeaders().get("Authorization", "").split(".")[1]))
						val ail = tokenMap["ail"]

						Mono
							.from(
								MongodbUtil.getCollection(AppService.COLLECTION_NAME)
									.updateOne(D("_id", D("\$in", ail)), D("\$inc", D("csv_count", data.size)))
							)
							.doOnError {
								log.error(it.message, it)
							}
							.subscribe() // 비동기 처리

						csvString.toString()
					}else{
						resp.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
						Util.toJsonString(it)
					}
				}
			resp.sendString(mono, Charset.forName("utf8"))
		}
	}

	private val serviceMap = mapOf(
		"/v3/user/get" to MemberService::get,
		"/v3/user/delete" to MemberService::delete,
		"/v3/user/put" to MemberService::put,
		"/v3/user/login" to MemberService::login,
	)

	/**
	 * 각 서비스에 연결하기 위해 파라미터 생성과 공통의 리스펀스를 처리 한다.
	 */
	fun websocket(wsi: WebsocketInbound, wso: WebsocketOutbound, isAuth: Boolean = true): Publisher<Void> {
		val mono = wsi.receive()
			.flatMap {
				val req = SafeRequest(wsi, it)
				val cmd = req.getBodyOrDefault("cmd", "")
				serviceMap[cmd]?.invoke(req) ?:
					throw PolarisException.status400BadRequest("Service not found!! [$cmd]")
			}
			// TODO 오류나면 connection close 하고 OnErrorResume 함수를 실행하지 않는다. 이유를 못찾겠네
			.onErrorResume {
				Mono.just(Status.status500InteralServerError(it))
			}

		return wso.sendString(mono.map { Util.toJsonString(it) })
	}

}
