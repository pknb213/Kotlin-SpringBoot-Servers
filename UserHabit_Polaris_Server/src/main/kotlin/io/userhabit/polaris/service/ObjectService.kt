package io.userhabit.polaris.service

import io.userhabit.batch.indicators.*
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P


/**
 * 화면분석
 * @author cjh
 */
object ObjectService {
	private val log = Loggers.getLogger(this.javaClass)
	// @deprecated
	// lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>

	/**
	 * 오브젝트 정보
	 * @author cjh
	 * @comment 21.11.18 yj
	 * @sample GET {{localhost}}/v3/object/{ids}
	 * @return data=[...]
	 * {
		"_id": {
			"oi": "Button0",
			"vhi": 0
		},
		"av": "3.6.1",
		"alias": null
	 * }
	 *
	 */
	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val objectIdList = req.splitParamOrDefault("ids", "")
		val appIdList = req.splitQueryOrDefault("app_id_list", "")

		val viewIdList = req.splitQueryOrDefault("view_id_list", "")
		val versionList = req.splitQueryOrDefault("version_list", "")

		val skip = req.getQueryOrDefault("skip", 0)
		val limit = req.getQueryOrDefault("limit", 40)
		val fieldList = req.splitQueryOrDefault("field_list", "")

		val vali = Validator()
			.new(limit, "limit").max(100)

		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return AppService.getAppIdList(P.uiK, req.getUserId())
			.flatMap { ownedAppIdList ->
				val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
					if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
				}

				val matchQuery = D()
					.append(P.ai, D("\$in", QueryPeasant.convertToObjectIdList(appIdList)))

				if(versionList.isNotEmpty()) {
					matchQuery.append(P.av, D("\$in", versionList))
				}

				if (viewIdList.isNotEmpty()) {
					matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
				}

				if (objectIdList.isNotEmpty()) {
					if(objectIdList[0] != "*"){
						matchQuery.append(P.oi, D("\$in", objectIdList))
					}
				}
				val (coll, query) = ObjectList.COLLECTION_NAME_PT24H to listOf(
					D("\$match", matchQuery),

					D("\$addFields", D(P.avK, D("\$map",
						D("input",D("\$split", listOf("\$${P.av}", ".")))
							.append("as","t")
							.append("in",D("\$toInt","\$\$t"))
					))),
					D("\$sort",D("${P.avK}.0",-1).append("${P.avK}.1",-1).append("${P.avK}.2",-1).append("${P.avK}.3",-1)),

					D("\$group", D()
						.append("_id", D()
							.append(P.oi, "\$${P.oi}")
							.append(P.vhi, "\$${P.vhi}"))
						.append("data", D("\$push", D()
							.append(P.av, "\$${P.av}")
							.append("path", "\$path")
							.append("alias", "\$alias")
							.append("favorite", "\$favorite") // Todo: object_list에 favorite이 없는데?
						))),

					D("\$project", D()
						.append("data", D("\$arrayElemAt", listOf("\$data", 0)))),

//					D("\$project", fieldList.fold(D()){ acc, it -> acc.append(it, 1)}
//						.let {
////							it.append("_id", D("\$toString", "\$_id")) // Todo: Object는 String 변환 할 수 없다.
//							it.append("_id", "\$_id") // Todo: vhi가 모두 0이니, _id.oi로 해도 되지 않을까?
//							if(it.containsKey("created_date")) it.append("created_date", D("\$dateToString", D("date", "\$created_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
//							if(it.containsKey("updated_date")) it.append("updated_date", D("\$dateToString", D("date", "\$updated_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
//							it
//						} ),
					// Todo: data: [_id,data]에서 data 내부의 map을 밖으로 꺼냄
					D(mapOf("\$replaceWith" to mapOf("\$mergeObjects" to listOf(
						mapOf("_id" to "\$_id"),
						"\$data"
					)))),
					D("\$project", mapOf(
						"_id" to 0,
						"oi" to "\$_id.oi",
						"vhi" to "\$_id.vhi",
						"av" to "\$av",
						"path" to "\$path"
					)),
					D("\$skip", skip),
					D("\$limit", limit)
				)

//				println(Util.toPretty(query))

				Flux
					.from(MongodbUtil.getCollection(coll).aggregate(query))
					.collectList()
					.doOnError { log.info(Util.toPretty(query)) }
					.map {
						Status.status200Ok(it)
					}

				// end of getAppIdList()
			}
	} // end of get()

	/**
	 * @author cjh
	 * @comment 21.11.18 yj
	 * @sample GET {{localhost}}/v3/object/{ids}/count?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z&view_hash_id=0
	 * @return data=[...]
	 * {
		"object_id": "userhabit_no_id",
		"value": 14871,
		"ratio": 54.59451521715187,
		"_id": {
			"timestamp": 1636615586,
			"date": 1636615586000
		},
		"ai": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
		"av": "3.6.1",
		"oi": "userhabit_no_id",
		"vhi": -1440253318,
		"alias": null,
		"path": "s3://service-uh-polaris-stage-2/attach/000000000000000000000004/1.0.1/-68199025",
		"app_version": [
			3,
			6,
			1
		]
	* }, ...
	 */
	fun count(req: SafeRequest): Mono<Map<String, Any>> {
		val appIdList = req.splitQueryOrDefault("app_id_list", "")
		val versionList = req.splitQueryOrDefault("version_list", "")
		val fromDate = req.getQueryOrDefault("from_date", "")
		val toDate = req.getQueryOrDefault("to_date", "")

		val viewId = req.getQueryOrDefault(P.vhiK, "")
		val afterViewId = req.getQueryOrDefault(P.avhiK, "")

		val sortField = req.getQueryOrDefault("sort_key", "value")
		val sortValue = req.getQueryOrDefault("sort_value", -1)
		val skip = req.getQueryOrDefault("skip", 0)
		val limit = req.getQueryOrDefault("limit", 40)
		val fieldList = req.splitQueryOrDefault("field_list", "")

		val by = req.getQueryOrDefault("by", "view")

		val vali = Validator()
//			.new(appIdList, "app_id_list").required()
			.new(fromDate, "from_date").required().date()
			.new(toDate, "to_date").required().date()
			.new(limit, "limit").max(100)
			.new(viewId, P.vhiK).required()

		if(by == "flow") {
			vali
				.new(afterViewId, P.avhiK).required()
		}
		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		val versionDoc = if(versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()

		return AppService.getAppIdList(P.uiK, req.getUserId())
			.flatMap { ownedAppIdList ->
				val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
					if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
				}
				val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

				val (coll, query) = if(by == "view"){

					val matchQuery = D()
						.append(P.stz, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))) )
						.append(P.ai, D("\$in", appIdInObjIds))

					if(versionList.isNotEmpty()) {
						matchQuery.append(P.av, D("\$in", versionList))
					}

					if(viewId != ""){ // Todo: 지금 indicator_object_by_view에 쌓이는 모든 데이터의 vhi필드는 0인데, 데이터가 잘못된걸까 로직이 잘못된걸까?
						matchQuery.append(P.vhi, D("\$eq", viewId.toLong()))
					}

					IndicatorObjectByView.COLLECTION_NAME_PT24H to
						listOf(
							D("\$match", matchQuery),

							D("\$group", D()
								.append("_id", D()
									.append(P.vhi, "\$${P.vhi}")
									.append(P.oi, "\$${P.oi}"))
								.append(P.oco, D("\$sum", "\$${P.oco}"))),

							D("\$group", D()
								.append("_id", 0)
								.append("data", D()
									.append("\$push", D()
										.append(P.vhi, "\$_id.${P.vhi}")
										.append(P.oi, "\$_id.${P.oi}")
										.append(P.oco, "\$${P.oco}")))
								.append("total_${P.oco}", D("\$sum", "\$${P.oco}"))),

							D("\$unwind", "\$data"),

							D("\$lookup", D()
								.append("from", ObjectList.COLLECTION_NAME_PT24H)
								.append("let" , D(P.vhi, "\$data.${P.vhi}").append(P.oi, "\$data.${P.oi}"))
								.append("pipeline", listOf(
									D("\$match", D("\$expr", D("\$and", listOf(
										D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
										D("\$eq", listOf("\$${P.oi}", "\$\$${P.oi}")),
										D("\$in", listOf("\$${P.ai}", appIdInObjIds)),
										versionDoc)))),
									D("\$addFields", D(P.avK, D("\$map",
										D("input",D("\$split", listOf("\$${P.av}", ".")))
											.append("as","t")
											.append("in",D("\$toInt","\$\$t"))
									))),
									D("\$sort",D("${P.avK}.0",-1).append("${P.avK}.1",-1).append("${P.avK}.2",-1).append("${P.avK}.3",-1)),
									D("\$limit", 1)
								))
								.append("as", "object")),

							D("\$replaceWith", D("\$mergeObjects", listOf( D()
								.append(P.oiK, "\$data.${P.oi}")
								.append("value", "\$data.${P.oco}")
								.append("ratio", D("\$multiply", listOf(D("\$divide", listOf("\$data.${P.oco}", "\$total_${P.oco}")), 100)))
								,
								D("\$arrayElemAt", listOf("\$object", 0))
							))),

							D("\$sort", D(sortField, sortValue)),

							D("\$skip", skip),
							D("\$limit", limit)
						)

				}
				else if(by == "flow"){

					val matchQuery = D()
						.append(P.stz, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))) )
						.append(P.ai, D("\$in", appIdInObjIds))

					if(versionList.isNotEmpty()) {
						matchQuery.append(P.av, D("\$in", versionList))
					}

					if(viewId != ""){
						matchQuery.append(P.vhi, D("\$eq", viewId.toLong()))
					}
					if(afterViewId != ""){
						matchQuery.append(P.avhi, D("\$eq", afterViewId.toLong()))
					}

					IndicatorByTrackingFlow.COLLECTION_NAME_PT24H to
						listOf(
							D("\$match", matchQuery),

							D("\$group", D()
								.append("_id", D()
									.append(P.vhi, "\$${P.vhi}")
									.append(P.t, "\$${P.t}")
									.append(P.oi, "\$${P.oi}"))
								.append(P.mco, D("\$sum", "\$${P.mco}"))),

							D("\$lookup", D()
								.append("from", ObjectList.COLLECTION_NAME_PT24H)
								.append("let" , D(P.vhi, "\$data.${P.vhi}").append(P.oi, "\$data.${P.oi}"))
								.append("pipeline", listOf(
									D("\$match", D("\$expr", D("\$and", listOf(
										D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
										D("\$eq", listOf("\$${P.oi}", "\$\$${P.oi}")),
										D("\$in", listOf("\$${P.ai}", appIdInObjIds)),
										versionDoc)))),
									D("\$addFields", D(P.avK, D("\$map",
										D("input",D("\$split", listOf("\$${P.av}", ".")))
											.append("as","t")
											.append("in",D("\$toInt","\$\$t"))
									))),
									D("\$sort",D("${P.avK}.0",-1).append("${P.avK}.1",-1).append("${P.avK}.2",-1).append("${P.avK}.3",-1)),
									D("\$limit", 1)
								))
								.append("as", "object")),

							D("\$replaceWith", D("\$mergeObjects", listOf( D()
								.append(P.oiK, "\$_id.${P.oi}")
								.append(P.tK, "\$_id.${P.t}")
								.append(P.mcoK, "\$${P.mco}")
								,
								D("\$arrayElemAt", listOf("\$object", 0))
							))),

							D("\$sort", D(sortField, sortValue)),

							D("\$skip", skip),
							D("\$limit", limit)
						)
				}
				else{
					"" to listOf(D())
				}

//				println(Util.toPretty(query))

				Flux
					.from(MongodbUtil.getCollection(coll).aggregate(query))
					.collectList()
					.doOnError { log.info(Util.toPretty(query)) }
					.map {
						Status.status200Ok(it)
					}

			} // end of getAppIdList()
	} // end of count()

}
