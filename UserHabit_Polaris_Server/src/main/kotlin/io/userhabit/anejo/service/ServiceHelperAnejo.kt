package io.userhabit.anejo.service

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import io.userhabit.common.SafeRequest
import io.userhabit.common.Status
import io.userhabit.common.Util
import io.userhabit.polaris.service.AppService
import reactor.core.publisher.Mono
import reactor.netty.NettyOutbound
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.util.Loggers
import java.time.ZonedDateTime
import org.bson.Document as D

/**
 * @author sbnoh
 *
 */
object ServiceHelperAnejo {
	private val log = Loggers.getLogger(this.javaClass)
	private val tempByteBuf = Unpooled.copiedBuffer("".toByteArray())

	fun http(serviceFn: (SafeRequest, Long) -> Mono<Map<String, Any>>):
			(HttpServerRequest, HttpServerResponse) -> NettyOutbound {

		return {req: HttpServerRequest, resp: HttpServerResponse ->

			val issuedAt = System.currentTimeMillis()
			val apikey = req.param("apikey") ?: ""

			val mono = Mono
				.zip(
					// TODO SafeRequest 다른 함수에서 실행하면 refCnt = 0 으로 됨.(아마도 쓰레드 관련...) / io.netty.util.IllegalReferenceCountException: refCnt: 0
					req.receive().aggregate().defaultIfEmpty(SafeRequest.emptyByteBuf).map { SafeRequest(req, it) },
					AppService.getAppIdMap(listOf(apikey))
				)
				.flatMap {
					val safeReq = it.t1
					if(it.t2.isEmpty()){
						log.info("apiKey [${apikey}] 는 등록되지 않은 key 입니다...");
						resp.status(HttpResponseStatus.NOT_FOUND) // rest api 설계상 이 상태는 안맞는데 아네호 호환을 위해 사용.
						Mono.just(
							mapOf(
								"status" to "Not Found",
								"message" to "App can't be found",
								"comment" to "-",
							)
						)
					}else{
						val body = if(safeReq.getHeaderOrDefault(HttpHeaderNames.CONTENT_TYPE.toString()).startsWith(HttpHeaderValues.APPLICATION_JSON)){
							safeReq.getBodyJsonToDocument()
						}else{
							val d = safeReq.getBodyOrDefault("d")
							if(d.trim()[0] == '{') D.parse(safeReq.getBodyOrDefault("d"))
							else D()
						}

						val st = body["s", D()]["t", 0L]
						if(st > 0 && st < ZonedDateTime.now().minusMonths(3).toInstant().toEpochMilli()){
							resp.status(HttpResponseStatus.NOT_FOUND)// rest api 설계상 이 상태는 안맞는데 아네호 호환을 위해 사용.
							Mono.just(
								mapOf(
									"status" to "Date error",
									"message" to "Past time",
									"comment" to "-",
								)
							)
						}else{
							serviceFn(safeReq, issuedAt)
						}
					}
				}
				.onErrorResume {
					log.error(it.toString(), it)
					resp.status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
					Mono.just(Status.status500InteralServerError(it))
				}
				.map {
					Util.toJsonString(it)
				}

			resp
				.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
				.sendString(mono)
		}

	}

}
