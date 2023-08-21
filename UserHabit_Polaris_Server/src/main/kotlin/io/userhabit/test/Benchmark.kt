package io.userhabit.test

import io.netty.handler.codec.http.HttpResponseStatus
import io.userhabit.Server
import io.userhabit.anejo.service.CloseService
import io.userhabit.common.Config
import io.userhabit.common.Util
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.util.Loggers
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.streams.toList
import io.userhabit.polaris.Protocol as P

object Benchmark {
	private val log = Loggers.getLogger(this.javaClass)

	@Suppress("UNCHECKED_CAST")
	@Throws(Exception::class)
	@JvmStatic
	fun main(args: Array<String>) {
		val startTime = Instant.now()

		/*
		convert the protocol from v2 to v3
		 */
		val sessionList = CloseService.resetSessionList(
			Util.getResource("test/0109_20200107.json.log")
//			Util.getResource("test/test")
			{
				Files.lines(it)
					.map {
						Util.jsonToMutableMap(it)
					}
					.toList()
			}
		)

		/*
		counting
		 */
		val count = AtomicInteger(0)
		var baseCount = 0
		val limit = System.getProperty("limit", "1").toLong()
		val maxConn = System.getProperty("maxconn", "1000").toInt()

		Flux.interval(Duration.ofSeconds(1))
			.subscribe {
				val cc = count.get()
				println("seconds[${it}], count[${cc - baseCount}]")
				baseCount = cc
			}

		val lineNum = sessionList.size
		val repeatNum = ((limit / lineNum)+1).toInt()
		log.info("limit=${limit} maxConn=${maxConn} fileLineNum=${lineNum} repeatNum=${repeatNum}")

		/*
		transfer start
		 */
		val cp = ConnectionProvider.builder("benchmark")
			.maxConnections(maxConn)
			.pendingAcquireMaxCount(100000) // TODO 줄여보자
			.build()

		val httpClient = HttpClient.create(cp).compress(true)
//		val sessionUrl = "${Config.get("http.host")}:${Config.get("http.port")}/v3/session"
//		val eventUrl = "${Config.get("http.host")}:${Config.get("http.port")}/v3/event"
		val sessionEventUrl = "${Config.get("http.host")}:${Config.get("http.port")}/v3/session_event"
		val day = 1000L * 60 * 60 * 24  // 24시간

		Flux.range(0, repeatNum)
			.flatMap {
				val plusDay = it * day
				Flux.fromIterable(sessionList)
					.map { session ->
						val session = session.toMutableMap() // copy, sessionList 재활용 하기 때문에!
						session[P.si] = "${it}_${session[P.si]}"
//						session[P.st] = (session[P.st] as Long) //- plusDay
						session
					}
//					.cache() // TODO 좀더 테스트 해보자
			}
			.take(limit)
			.flatMap {session ->
//				val eventList = session.remove("e") as List<*>

//				println(Util.toPretty(session))
//			    println(Util.toPretty(eventList))

				count.addAndGet(1)
//				Flux.zip(
//					httpClient
//						.post()
//						.uri(sessionUrl)
//						.send(ByteBufFlux.fromString(Mono.just(Util.toJsonString(listOf(session)))))
//						.responseContent()
//						.aggregate()
//						.asString(),
//					httpClient
//						.post()
//						.uri(eventUrl)
//						.send(ByteBufFlux.fromString(Mono.just(Util.toJsonString(eventList))))
//						.responseContent()
//						.aggregate()
//						.asString()
//				)
				httpClient
					.post()
					.uri(sessionEventUrl)
					.send(ByteBufFlux.fromString(Mono.just(Util.toJsonString(listOf(session)))))
					.responseSingle{ resp, bbm ->
						bbm
							.asString()
							.defaultIfEmpty("")
							.map {
								if(resp.status().code() != HttpResponseStatus.OK.code() || it.isEmpty() || Util.jsonToMap(it)["status"] != 200L){
									log.error("${resp.status()} / ${resp.toString()} ${it}")
								}
							}
					}
			}
			.doOnError {
				log.error(sessionEventUrl, it)
			}
			.blockLast()
		Thread.sleep(1000)

		val du = Duration.between(startTime, Instant.now())
		println("\nTotal transmission count [${count.get()}], duration [${String.format("%d:%02d:%02d", du.toHours(), du.toMinutesPart(), du.toSecondsPart())}]")
	}

	init {
		Server.logInit()
	}
}
