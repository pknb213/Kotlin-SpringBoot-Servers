package io.userhabit.anejo.service

// TODO jackson to gson
//import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import io.userhabit.common.SafeRequest
import io.userhabit.common.Util
import io.userhabit.polaris.service.SessionEventService
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import io.userhabit.polaris.EventType as ET
import io.userhabit.polaris.Protocol as P

/**
 * @author sbnoh
 * @see collection of anejo project 'io/userhabit/anejo/component/request/Session2UpdateRequestHandler.java'
 */
object CloseService {
	private val log = Loggers.getLogger(CloseService.javaClass)

	fun post(req: SafeRequest, issuedAt: Long): Mono<Map<String, Any>> {
		val sessionListRaw = listOf(req.getBodyOrDefault("d").let {
			Util.jsonToMap(it)
		})

		val actionList = (req.getFile("f")["body"] as ByteBuffer).let {
			Util.toInputStream(it).use { bais ->
				GZIPInputStream(bais).use {gis ->
					BufferedReader(InputStreamReader(gis)).readLines().map{
						Util.jsonToMap(it)
					}
				}
			}
		}

		val sessionList = resetSessionList(
			sessionListRaw.mapIndexed {i, s ->
				s as MutableMap
				s["f"] =  actionList // resetEventList에서 f > e 로 바뀜
				s
			}
		)

		val req = req.getNew(mapOf(), mapOf(), Util.toJsonString(sessionList))

		return SessionEventService.postAndPut(req)
	}

	fun mapToFlat(childMap: Map<String, Any>, parentKey: String = "", resultMap: MutableMap<String, Any> = mutableMapOf() ): MutableMap<String, Any>{
		childMap.forEach {
			val k = it.key
			val v = it.value
			if(v is Map<*, *>){
				@Suppress("UNCHECKED_CAST")
				mapToFlat(v as MutableMap<String, Any>, parentKey.plus(k), resultMap )
			}else{
				resultMap[parentKey.plus(k)] = v
			}
		}
		return resultMap
	}

	fun resetSessionList(sessionList: List<MutableMap<String, Any>>): List<MutableMap<String, Any>> {
		val calcTime = System.currentTimeMillis() - sessionList.first()["s"].let { (it as Map<*,*>)["t"] as Long} //+ (1000 * 60 * 60 * 8)

		return sessionList.map { s ->
			s.remove("sv") // mapFlatMap 하기전 sv 값이 있음
			val eventList = s.remove("f") as List<MutableMap<String, Any>>
			val session = mapToFlat(s)

//			session.remove(P.si)?.let { session["_id"] = it}
			session[P.st] = (session[P.st] as Long) + calcTime
			session[P.i] = System.currentTimeMillis() // 서버 입력 시간// 벤치마크에서 전송할 때 배치 처리에 영향을 안주기 위해
			session.remove("an")?.let { session[P.av] = it}
			session.remove("ac")?.let { session[P.ab] = it}
			session.remove("as")?.let { session[P.usv] = it}
			session.remove("aa")?.let { session[P.ak] = it}
			session.remove("dswh")
			session.remove("sws")
			session.remove("sd")
			session.remove("so")
			session.remove("su")
			session.remove("sv")?.let {session[P.dov] = it}
			session.remove("sl")?.let {session[P.dl] = it}
			session.remove("sz")?.let {session[P.dz] = it}
			session.remove("sc")?.let {session[P.se] = it}
			session.remove("v")
			session.remove("n")
			session.remove("at")
			session.remove("sts")
			session.remove("eds")
			session.remove("cgm")
			session.remove("dsw")?.let { session[P.dw] = it }
			session.remove("dsh")?.let { session[P.dh] = it }

			// sdk에서 전송하지 않는 필드 값 삭제 = 기존 서버에서 생성 된 필드
			session.remove("ss")
			session.remove("isc")
			session.remove("ic")
			session.remove("lc")
			session.remove("ln")
			session.remove("ls")
			session.remove("lt")
			session.remove("p")
			session.remove("uv")
			session.remove("w")

			session.remove("cb")
			session.remove("cc")
			session.remove("cfd")
			session.remove("cfm")
			session.remove("cj")
			session.remove("cm")
			session.remove("cn")
			session.remove("cp")
			session.remove("cs")
			session.remove("ct")
			session.remove("ctd")
			session.remove("ctm")

			session.remove("m")

//				session.remove("av") // SDK 버전
//				session.remove("dswh") // 해상도
//				session.remove("sws") // 서버에서 생성한 세션 시간
//				session.remove("so") // timezone offset
//				session.remove("sd") // 앱 시작 시간
//				session.remove("su") // 세션 시작 활성화 시간
//				session.remove("v") // 버전 정보 (디비 키)
//				session.remove("n") // 네트워크 상태 (중복)
//				session.remove("at") // DB 들어가기 전 후 (레거시)
//				session.remove("sv") // 스크린 뷰
//				session.remove("sts") // 시작 화면
//				session.remove("eds") // 종료 화면
//				session.remove("cgm")

			// 아네호의 액션이 폴라리스에서 이벤트
			session[P.e] = resetEventList(eventList)
			session
		}

	}

	private fun resetEventList(eventList: List<MutableMap<String, Any>> ): List<MutableMap<String, Any>> {
		return eventList
			.flatMapIndexed { i, mm ->
				val event = mapToFlat(mm)
				val type = ET.convertToPolaris((event[P.t] as Number).toInt())
				event[P.t] = type
				event[P.ts] = (event.remove("u") as Number).toLong().times(1000)
				event.remove("ma")?.let{
					if(type == ET.VIEW_START) event["ea"] = it
					if(type == ET.USER_CONTENT) event[P.odk] = it
				}
				event.remove("ea")?.let{ event[P.vi] = it.toString() + (event.remove("mf") ?: "")}
				event.remove("mr")?.let{ event[P.vo] = it}
				event.remove("mpwh")
				event.remove("mpw")?.let{ event[P.vw] = it}
				event.remove("mph")?.let{ event[P.vh] = it}
				event.remove("mp")?.let{
					if((type == ET.REACT_TAP || type == ET.REACT_DOUBLE_TAP || type == ET.REACT_LONG_TAP ||type == ET.REACT_SWIPE) && it is List<*>) { event[P.gx] = it[0]!!; event[P.gy] = it[1]!! }
					if(type == ET.CRASH) event[P.cp] = it// crash process
					if((type == ET.SCROLL_CHANGE || type == ET.SCROLL_END) && it is List<*>){ event[P.spx] = it[0]!!; event[P.spy] = it[1]!! }
				}
				event.remove("ms")?.let{
					if(type == ET.REACT_TAP && it is List<*>) { event[P.spx] = (it[0] as Number).toInt(); event[P.spy] = (it[1] as Number).toInt()}
					if(type == ET.CRASH) event[P.cs] = it // stacktrace
				}
				event.remove("mo")?.let{
					if(type == ET.REACT_TAP || type == ET.REACT_DOUBLE_TAP || type == ET.REACT_LONG_TAP ||type == ET.REACT_SWIPE) event[P.oi] = it
					if(type == ET.USER_CONTENT) event[P.odv] = it
				}
				event.remove("md")?.let{ event[P.od] = it}
				event.remove("mc")?.let{
					if((type == ET.REACT_TAP || type == ET.REACT_DOUBLE_TAP || type == ET.REACT_LONG_TAP || type == ET.REACT_SWIPE) && it is String) event[P.svi] = it
					if(type == ET.SCROLL_CHANGE) event[P.svi] = it
					if(type == ET.CRASH) event[P.cc] = it // crash charging
					if(type == ET.SCROLL_END) event[P.svi] = it
				}
				event.remove("mt")?.let{
					if(it is List<*>) { event[P.gex] = it[0]!!; event[P.gey] = it[1]!! }
					if(type == ET.CRASH) event[P.ct] = it // crash exception (issue)
				}
				event.remove("mb")?.let{
					if(it is List<*>) { event[P.spx] = it[0]!!; event[P.spy] = it[1]!! }
					if(type == ET.CRASH) event[P.cb] = it // crash battery
				}
				event.remove("mi")?.let{ event[P.gv] = it}

				// crash
				if(type == ET.CRASH) {
					event.remove("mm")?.let{ event[P.cm] = it }
					event.remove("mgm")?.let{ event[P.cg] = it }
					event.remove("mfd")?.let{ event[P.cfd] = it }
					event.remove("mfm")?.let{ event[P.cfm] = it }
					event.remove("mj")
					event.remove("mn")?.let{ event[P.cn] = it }
					event.remove("mtd")?.let{ event[P.ctd] = it }
					event.remove("mtm")?.let{ event[P.ctm] = it }
				}

				event.remove("mkbn")
				event.remove("mkbp")
				event.remove("mkbs")
				event.remove("mkbk")

				if(type == ET.APP_START) event[P.vi] = "###SESSION_START###"
				if(type == ET.APP_END) event[P.vi] = "###SESSION_END###"

				val typeNext = ET.convertToPolaris(((eventList.elementAtOrNull(i+1)?.getOrDefault(P.t, 0) ?: 0 ) as Number).toInt())
				if((typeNext == ET.VIEW_START || typeNext == ET.APP_END) && type != ET.APP_START){ // 뷰 시작이나 앱 끝 이면, // 앱 시작이 아니면
					listOf(event, mutableMapOf(
						P.ts to (event[P.ts] as Number).toLong().plus(1),
						P.t to ET.VIEW_END
					))
				}else{
					listOf(event)
				}
			}
	}
}
