package io.userhabit.anejo.service

import io.netty.handler.codec.http.HttpResponseStatus
import io.userhabit.common.SafeRequest
import io.userhabit.common.Util
import io.userhabit.polaris.service.AppService
import io.userhabit.polaris.service.EventService
import io.userhabit.polaris.service.StorageService
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.nio.ByteBuffer
import io.userhabit.polaris.Protocol as P
import org.bson.Document as D

/**
 * @author sbnoh
 *
 */
object ScreenShotService {
	private val log = Loggers.getLogger(this.javaClass)

	/**
	 * @see collection of anejo project 'io/userhabit/anejo/component/request/ScreenshotIndexRequestHandler.java'
	 */
	fun getScreenshot(req: SafeRequest, issuedAt: Long): Mono<Map<String, Any>> {
// 아네호 api 호출한 response 값
//		{
//			"status": "OK",
//			"serverTime": 1625493724065,
//			"screenList": [
//			{
//				"a": "com.hdsec.android.mainlib.SmartActivity",
//				"f": "SI321000",
//				"r": 1,
//				"o": null
//			},
//			{
//				"a": "com.hdsec.android.mainlib.SmartActivity",
//				"f": "SI3210P0",
//				"r": 1,
//				"o": null
//			},
//		  ...
//			{
//				"a": "com.hdsec.android.mainlib.SmartActivity",
//				"f": "SI324000",
//				"r": 1,
//				"o": null
//			}
//			],
//			"screenObjectList": [
//			{
//				"a": "com.hdsec.android.mainlib.SmartActivity",
//				"f": "뱅킹/대출",
//				"r": -1,
//				"o": [
//				{ "i": "" },
//				{ "i": "bd_cell0" },
//				{ "i": "Button0" },
//				{ "i": "com.kbsec.mts.kbfont:id/iv_menu" },
//				{ "i": "Label3" }
//				]
//			},
//			{
//				"a": "com.hdsec.android.mainlib.SmartActivity",
//				"f": "금융상품",
//				"r": -1,
//				"o": [
//				{ "i": "bd_cell0" },
//				{ "i": "bd_cell_likecount" },
//				{ "i": "lblTitle" }
//				]
//			},
//		  ...
//			{
//				"a": "com.hdsec.android.mainlib.SmartActivity",
//				"f": "투자정보",
//				"r": -1,
//				"o": [
//				{ "i": "" },
//				{ "i": "bd_cell0" },
//				{ "i": "com.kbsec.mts.kbfont:id/lv_left_menu::ITEM" },
//				{ "i": "lblTitle" }
//				]
//			}
//			],
//		"take": 3,
//		"method": 1
//		}

		val apikey = req.getParamOrDefault("apikey", "")

		@Suppress("UNCHECKED_CAST")
		val mono = AppService.getAppIdMap(listOf(apikey))
			.flatMap {
				val siList = listOf(it[apikey]!!)
				val body = req.getBodyJsonToDocument()
				val req = req.getNew(mapOf(
					// storage = vi oi
					// event = vo
					"field_list" to listOf("${P.vi},${P.oi},${P.vo}"),
					// storage
					P.ai to siList,
					P.av to listOf(body["a", D()]["n", ""]),
					// event
					"session_id_list" to siList,
//					"limit" to listOf("1"),
				), mapOf())

				Mono.zip(
					StorageService.get(req),
					EventService.get(req)
				)
			}
			.map {
				val storage = it.t1
				val status = storage["status"] as Int
				val storageData = storage["data"] as List<Map<String, String>>
				val event = it.t2
				val eventData = (event["data"] as List<Map<String, Any>>).let { if(it.size > 0) it.first() else mapOf() }
				val pair = storageData.partition { it.getOrDefault(P.oi, "").isEmpty() }

				val screenList = pair.first.map { // != oi
					val AF = it.getOrDefault(P.vi, "").split("::")
					mapOf(
						"a" to AF.first(),
						"f" to if(AF.size > 1) AF[1] else "",
						"r" to eventData.getOrDefault(P.vo, -1), // 1 세로, 2 가로
						"o" to null
					)
				}

				val objectList = pair.second // == oi
//					.filter { it.getOrDefault(P.oi , "").isNotEmpty() }
					.fold(mutableMapOf<String, Map<String, Any>>()) { acc, m ->
						val vi = m[P.vi]!!
						val obj = acc[vi]
						if(obj != null)
							obj["o"].let{
								it as MutableList<Map<String, String>>
								it.add(mapOf( "i" to m[P.oi]!!))
							}
						else{
							val AF = vi.split("::")
							acc[vi] = mapOf(
								"a" to AF.first(),
								"f" to if(AF.size > 1) AF[1] else "",
								"r" to -1, // why hard coding??
								"o" to mutableListOf(mapOf("i" to m[P.oi]))
							)
						}

						acc
					}
					.values
					.toList()

				val isDevApp = apikey.startsWith("dev_")

				mapOf(
					"status" to HttpResponseStatus.valueOf(status).reasonPhrase(),
					"message" to storage.getOrDefault("message", ""),
					"serverTime" to System.currentTimeMillis(),
					"screenList" to screenList,
					"screenObjectList" to objectList,
					"take" to if(isDevApp) 3 else 0, // why hard coding??
					"method" to 1 // why hard coding??
				)
			}

		return mono
	}

	/**
	 * @see stream of anejo project '/io/userhabit/anejo/stream/component/action/UploadScreenshotAction.java'
	 * @see manager of anejo project '/app/v1/images/ImageController.scala'
	 * @see common of anejo project '/io/userhabit/anejo/common/db/service/ScreenServiceImpl.java'
	 *
	 * process = collection > kafka > stream > manager
	 */
	fun postView(req: SafeRequest, issuedAt: Long): Mono<Map<String, Any>> {
//    아네호 sdk 리퀘스트 샘플 데이터
//		[
//	  	{"ak":"dev_b3068e50a8afca37a2909990f9b8c0f7efbe2168","vi":"{{viewId1}}","av":"{{appVersion1}}","vit":"jpg"},
//			{"ak":"dev_b3068e50a8afca37a2909990f9b8c0f7efbe2168","vi":"{{viewId2}}","av":"{{appVersion2}}","vit":"jpg"}]
//		[

		val metaString = req.getBodyOrDefault("d")
		val fileMap = Util
			.jsonToList(metaString)
			.foldIndexed(mutableMapOf<String, Map<String, Any>>()) { index, acc, map ->
				acc["file_${index}"] = req.getFile(map[P.vi] as String)
				acc
			}

		return StorageService
			.postAndPut(req.getNew( mapOf(), mapOf(), "", mapOf("meta" to metaString), fileMap))
			.map {
				mapOf<String, Any>(
					"status" to "OK",
					"serverTime" to System.currentTimeMillis(),
				)
			}
	}

	fun postObject(req: SafeRequest, issuedAt: Long): Mono<Map<String, Any>> {
//    아네호 sdk 리퀘스트 샘플 데이터
//		[
//			{"ak":"dev_b3068e50a8afca37a2909990f9b8c0f7efbe2168","vi":"{{viewId1}}","av":"{{appVersion1}}","vit":"jpg","oi":"{{objectId1}}"},
//			{"ak":"dev_b3068e50a8afca37a2909990f9b8c0f7efbe2168","vi":"{{viewId2}}","av":"{{appVersion2}}","vit":"jpg","oi":"{{objectId2}}"}
//		]
		val metaString = req.getBodyOrDefault("d")
		val fileMap = Util
			.jsonToList(metaString)
			.foldIndexed(mutableMapOf<String, Map<String, Any>>()) { index, acc, map ->
				acc["file_${index}"] = req.getFile(map[P.oi] as String)
				acc
			}

		return StorageService
			.postAndPut(req.getNew( mapOf(), mapOf(), "", mapOf("meta" to metaString), fileMap))
			.map {
				mapOf<String, Any>(
					"status" to "OK",
					"serverTime" to System.currentTimeMillis(),
				)
			}
	}

	fun postScroll(req: SafeRequest, issuedAt: Long): Mono<Map<String, Any>> {
//    아네호 sdk 리퀘스트 샘플 데이터
//		[
//			{
//				"ak":"dev_b3068e50a8afca37a2909990f9b8c0f7efbe2168",
//				"vi":"{{viewId1}}",
//				"av":"{{appVersion1}}",
//				"vit":"jpg",
//				"svi":"scroll",
//				"svn":6
//			}
//		]

		val metaList = Util.jsonToList(req.getBodyOrDefault("d"))
		// TODO metaList.size == 0 일 수도 있나?

		// concat file
		val file0 = req.getFile(metaList[0][P.svi] as String).let {
			// TODO svn 값이 순서가 보장되어 있나??
			val body = metaList
				.foldIndexed(ByteBuffer.allocate(0)) { index, acc, map ->
					val body = req.getFile(map[P.svi] as String)["body"] as ByteBuffer
					ByteBuffer.allocate(acc.limit() + body.limit()).put(acc).put(body).position(0)
				}

			it as MutableMap
			it["body"] = body
			it
		}
		val fileMap = mapOf("file_0" to file0)
		val metaString = Util.toJsonString(listOf(metaList[0]))

		return StorageService
			.postAndPut(req.getNew( mapOf(), mapOf(), "", mapOf("meta" to metaString), fileMap))
			.map {
				mapOf<String, Any>(
					"status" to "OK",
					"serverTime" to System.currentTimeMillis(),
				)
			}
	}
}
