package io.userhabit.polaris.service

import io.userhabit.batch.indicators.*
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.service.heatmap.HeatmapServiceActionType
import io.userhabit.polaris.service.heatmap.HeatmapServiceFirstLastTap
import io.userhabit.polaris.service.heatmap.HeatmapServiceScrollView
import io.userhabit.polaris.EventType as ET
import io.userhabit.polaris.Protocol as P
import org.bson.Document as D
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*

object HeatmapService {
    private val log = Loggers.getLogger(this.javaClass)
    // @deprecated
    // lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>

    /**
     *  @comment 21.11.18 yj
     *  @sample []
     *  @return data=[...]
     *  @see [Just Debug]
     */
    // Todo : Just Debug, Delete
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val viewIdList = req.splitQueryOrDefault("view_id_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val orientation = req.getQueryOrDefault("orientation", "1")
        val fieldList = req.splitQueryOrDefault("field_list", "")

        val by = req.getQueryOrDefault("by", "view")
        val type = req.getQueryOrDefault("type", "tap")

        val validator = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()

        if (validator.isNotValid()) throw PolarisException.status400BadRequest(validator.toExceptionList())

        val versionD = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()

        val typeD = when (type) {
            "tap" -> D("\$eq", ET.REACT_TAP)
            "double_tap" -> D("\$eq", ET.REACT_DOUBLE_TAP)
            "long_tap" -> D("\$eq", ET.REACT_LONG_TAP)
            "swipe" -> D("\$eq", ET.REACT_SWIPE)
            "no_response" -> D("\$in", listOf(ET.NOACT_TAP, ET.NOACT_LONG_TAP, ET.NOACT_DOUBLE_TAP, ET.NOACT_SWIPE))
            else -> D("\$eq", ET.REACT_TAP)
        }

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }
                Mono.just(mapOf("Heatmap" to "Test~"))
            }
    }

    /**
     * @author
     * @comment 21.11.19 yj
     * @sample GET {{localhost}}/v3/heatmap/{ids}/count?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z
     * @param
     * 		versionList: ...,
     * 		viewIdList: ...,
     * 		by: "view", "scroll_view", "reach_rate",
     * 		type: "tap", "swipe", "long_tap", "double_tap", "response", "no_response", "first_tap", "last_tap"
     * @return data=[{
     * "app_id": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
     * "view_hash_id": 306590330,
     * "view_id": "com.hdsec.android.mainlib.SmartActivitySM0200E2","alias": null,
     * "favorite": false,
     * "data": [
     * 		{
     * 			"hx": 0.24,
     * 			"hy": 0.21,
     * 			"count": 4
     * 		},
     * 		{
     * 			"hx": 0.24,
     * 			"hy": 0.21,
     * 			"count": 54
     * 		}
     * ]
     * }]
     */
    fun count(req: SafeRequest): Mono<Map<String, Any>> {
        val type = req.getQueryOrDefault("type", "tap")
        if (listOf(
                HeatmapServiceFirstLastTap.TYPE_FIRSTTAP,
                HeatmapServiceFirstLastTap.TYPE_LASTTAP
            ).indexOf(type) != -1) {
            // type : first_tap | last_tap
            return this.getFirstLastTapHeatmap(req)
        }

        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val viewIdList = req.splitQueryOrDefault("view_id_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val orientation = req.getQueryOrDefault("orientation", "1")
        val fieldList = req.splitQueryOrDefault("field_list", "")

        val by = req.getQueryOrDefault("by", "view")


        val vali = Validator()
//			.new(appIdList, "app_id_list").required()
//			.new(viewIdList, "view_id_list").required()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(by, "by").required(listOf("view", "scroll_view", "reach_rate"))
            .new(type, "type").required(listOf("tap", "swipe", "long_tap", "double_tap", "response", "no_response", "first_tap", "last_tap"))
            .new(orientation, "orientation").required(listOf("1", "2"))

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()

        val typeDoc = when (type) {
            "tap" -> D("\$eq", ET.REACT_TAP)
            "double_tap" -> D("\$eq", ET.REACT_DOUBLE_TAP)
            "long_tap" -> D("\$eq", ET.REACT_LONG_TAP)
            "swipe" -> D("\$eq", ET.REACT_SWIPE)
            "response" -> D("\$in", listOf(ET.REACT_TAP, ET.REACT_LONG_TAP, ET.REACT_DOUBLE_TAP, ET.REACT_SWIPE))
            "no_response" -> D("\$in", listOf(ET.NOACT_TAP, ET.NOACT_LONG_TAP, ET.NOACT_DOUBLE_TAP, ET.NOACT_SWIPE))
            else -> D("\$eq", ET.REACT_TAP)
        }

        val filterDoc: Any = when (type) {
            "first_tap" -> mapOf("\$arrayElemAt" to listOf("\$data", 0))
            "last_tap" -> mapOf("\$arrayElemAt" to listOf("\$data", -1))
            else -> "\$data"
        }

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

                // By는 View, React_rate, Scroll_view로 나뉨.
                val (coll, query) = if (by == "view") {
                    // Type은 Swipe, 그 외(React, Noact) 나뉨.
                    if (type == "swipe") {
                        val matchQuery = D()
                            .append(P.stz, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))))
                            .append(P.ai, D("\$in", appIdInObjIds))
                            .append(P.t, typeDoc)
                            .append(P.vo, D("\$eq", orientation.toInt()))

                        if (versionList.isNotEmpty()) {
                            matchQuery.append(P.av, D("\$in", versionList))
                        }

                        if (viewIdList.isNotEmpty()) {
                            matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
                        }

                        IndicatorHeatmapByView.COLLECTION_NAME_PT10M to listOf(
                            D("\$match", matchQuery),

                            D("\$group", D()
                                .append("_id", D()
                                    .append(P.ai, "\$${P.ai}")
                                    .append(P.vhi, "\$${P.vhi}"))
                                .append("data", D("\$push", D()
                                    .append("hx", "\$hx")
                                    .append("hy", "\$hy")
                                    .append("hex", "\$hex")
                                    .append("hey", "\$hey")
                                    .append("count", D("\$sum", "\$count"))))
                            ),

                            D("\$lookup", D()
                                .append("from", ViewList.COLLECTION_NAME_PT24H)
                                .append("let", D(P.vhi, "\$_id.${P.vhi}"))
                                .append("pipeline", listOf(
                                    D("\$match", D("\$expr", D("\$and", listOf(
                                        D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                        versionDoc)
                                    ))),
                                    D("\$addFields", D(P.avK, D("\$map",
                                        D("input", D("\$split", listOf("\$${P.av}", ".")))
                                            .append("as", "t")
                                            .append("in", D("\$toInt", "\$\$t"))
                                    ))),
                                    D("\$sort", D("${P.avK}.0", -1).append("${P.avK}.1", -1).append("${P.avK}.2", -1).append("${P.avK}.3", -1)),
                                    D("\$limit", 1)))
                                .append("as", "view")),

                            D("\$unwind", "\$view"),

                            D("\$project", D()
                                .append("_id", 0)
                                .append(P.aiK, "\$_id.${P.ai}")
                                .append(P.vhiK, "\$_id.${P.vhi}")
                                .append(P.viK, "\$view.${P.vi}")
                                .append("alias", "\$view.alias")
                                .append("favorite", "\$view.favorite")
                                .append("data", filterDoc)
                            )
                        )
                    } else {
                        val matchQuery = D()
                            .append(P.stz, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))))
                            .append(P.ai, D("\$in", appIdInObjIds)) // Todo : Token 체크
                            .append(P.t, typeDoc)
                            .append(P.vo, D("\$eq", orientation.toInt()))

                        if (versionList.isNotEmpty()) {
                            matchQuery.append(P.av, D("\$in", versionList))
                        }

                        if (viewIdList.isNotEmpty()) {
                            matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
                        }
                        IndicatorHeatmapByView.COLLECTION_NAME_PT10M to listOf(
                            D("\$match", matchQuery),

                            D("\$group", D()
                                .append("_id", D()
                                    .append(P.ai, "\$${P.ai}")
                                    .append(P.vhi, "\$${P.vhi}"))
                                .append("data", D("\$push", D()
                                    .append("hx", "\$hx")
                                    .append("hy", "\$hy")
                                    .append("count", D("\$sum", "\$count"))))
                            ),

                            D("\$lookup", D()
                                .append("from", ViewList.COLLECTION_NAME_PT24H)
                                .append("let", D(P.ai, "\$_id.${P.ai}").append(P.vhi, "\$_id.${P.vhi}"))
                                .append("pipeline", listOf(
                                    D("\$match", D("\$expr", D("\$and", listOf(
                                        D("\$eq", listOf("\$${P.ai}", "\$\$${P.ai}")),
                                        D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                        versionDoc
                                    )))),
                                    D("\$addFields", D(P.avK, D("\$map",
                                        D("input", D("\$split", listOf("\$${P.av}", ".")))
                                            .append("as", "t")
                                            .append("in", D("\$toInt", "\$\$t"))
                                    ))),
                                    D("\$sort", D("${P.avK}.0", -1).append("${P.avK}.1", -1).append("${P.avK}.2", -1).append("${P.avK}.3", -1)),
                                    D("\$limit", 1)
                                ))
                                .append("as", "view")),

                            D("\$unwind", "\$view"),

                            D("\$project", D()
                                .append("_id", 0)
                                .append(P.aiK, "\$_id.${P.ai}")
                                .append(P.vhiK, "\$_id.${P.vhi}")
                                .append(P.viK, "\$view.${P.vi}")
                                .append("alias", "\$view.alias")
                                .append("favorite", "\$view.favorite")
                                .append("data", filterDoc)
                            )
                        )
                    }
                } else if (by == "scroll_view") {
                    val matchQuery = D()
                        .append(P.stz, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))))
                        .append(P.ai, D("\$in", appIdInObjIds))
                        .append(P.t, typeDoc)

                    if (versionList.isNotEmpty()) {
                        matchQuery.append(P.av, D("\$in", versionList))
                    }

                    if (viewIdList.isNotEmpty()) {
                        matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
                    }

                    IndicatorHeatmapByScrollView.COLLECTION_NAME_PT24H to listOf(
                        D("\$match", matchQuery),

                        D("\$group", D()
                            .append("_id", D()
                                .append(P.ai, "\$${P.ai}")
                                .append(P.vhi, "\$${P.vhi}")
                                .append(P.svi, "\$${P.svi}")
                                .append("spx", "\$spx")
                                .append("spy", "\$spy"))
                            .append("count", D("\$sum", "\$count"))
                        ),

                        D("\$group", D()
                            .append("_id", D()
                                .append(P.ai, "\$_id.${P.ai}")
                                .append(P.svi, "\$_id.${P.svi}")
                                .append(P.vhi, "\$_id.${P.vhi}"))
                            .append("data", D("\$push", D()
                                .append("spx", "\$_id.spx")
                                .append("spy", "\$_id.spy")
                                .append("count", "\$count")))
                        ),

                        D("\$lookup", D()
                            .append("from", ViewList.COLLECTION_NAME_PT24H)
                            .append("let", D(P.ai, "\$_id.${P.ai}").append(P.vhi, "\$_id.${P.vhi}"))
                            .append("pipeline", listOf(
                                D("\$match", D("\$expr", D("\$and", listOf(
                                    D("\$eq", listOf("\$${P.ai}", "\$\$${P.ai}")),
                                    D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                    versionDoc)
                                ))),
                                D("\$addFields", D(P.avK, D("\$map",
                                    D("input", D("\$split", listOf("\$${P.av}", ".")))
                                        .append("as", "t")
                                        .append("in", D("\$toInt", "\$\$t"))
                                ))),
                                D("\$sort", D("${P.avK}.0", -1).append("${P.avK}.1", -1).append("${P.avK}.2", -1).append("${P.avK}.3", -1)),
                                D("\$limit", 1)))
                            .append("as", "view")),

                        D("\$unwind", "\$view"),

                        D("\$project", D()
                            .append("_id", 0)
                            .append(P.aiK, "\$_id.${P.ai}")
                            .append(P.vhiK, "\$_id.${P.vhi}")
                            .append(P.sviK, "\$_id.${P.svi}")
                            .append(P.viK, "\$view.${P.vi}")
                            .append("alias", "\$view.alias")
                            .append("favorite", "\$view.favorite")
                            .append("data", filterDoc)
                        )
                    )
                } else if (by == "reach_rate") {
                    val matchQuery = D()
                        .append(P.stz, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))))
                        .append(P.ai, D("\$in", appIdInObjIds))

                    if (versionList.isNotEmpty()) {
                        matchQuery.append(P.av, D("\$in", versionList))
                    }

                    if (viewIdList.isNotEmpty()) {
                        matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
                    }

                    IndicatorReachRateByScrollView.COLLECTION_NAME_PT24H to listOf(
                        D("\$match", matchQuery),

                        D("\$group", D()
                            .append("_id", D()
                                .append(P.ai, "\$${P.ai}")
                                .append(P.vhi, "\$${P.vhi}")
                                .append(P.svi, "\$${P.svi}")
                                .append("spx", "\$spx")
                                .append("spy", "\$spy"))
                            .append("count", D("\$sum", "\$count"))
                        ),

                        D("\$group", D()
                            .append("_id", D()
                                .append(P.ai, "\$_id.${P.ai}")
                                .append(P.svi, "\$_id.${P.svi}")
                                .append(P.vhi, "\$_id.${P.vhi}"))
                            .append("data", D("\$push", D()
                                .append("spx", "\$_id.spx")
                                .append("spy", "\$_id.spy")
                                .append("count", "\$count")))
                        ),

                        D("\$lookup", D()
                            .append("from", ViewList.COLLECTION_NAME_PT24H)
                            .append("let", D(P.ai, "\$_id.${P.ai}").append(P.vhi, "\$_id.${P.vhi}"))
                            .append("pipeline", listOf(
                                D("\$match", D("\$expr", D("\$and", listOf(
                                    D("\$eq", listOf("\$${P.ai}", "\$\$${P.ai}")),
                                    D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                    versionDoc)
                                ))),
                                D("\$addFields", D(P.avK, D("\$map",
                                    D("input", D("\$split", listOf("\$${P.av}", ".")))
                                        .append("as", "t")
                                        .append("in", D("\$toInt", "\$\$t"))
                                ))),
                                D("\$sort", D("${P.avK}.0", -1).append("${P.avK}.1", -1).append("${P.avK}.2", -1).append("${P.avK}.3", -1)),
                                D("\$limit", 1)))
                            .append("as", "view")),

                        D("\$unwind", "\$view"),

                        D("\$project", D()
                            .append("_id", 0)
                            .append(P.aiK, "\$_id.${P.ai}")
                            .append(P.vhiK, "\$_id.${P.vhi}")
                            .append(P.sviK, "\$_id.${P.svi}")
                            .append(P.viK, "\$view.${P.vi}")
                            .append("alias", "\$view.alias")
                            .append("favorite", "\$view.favorite")
                            .append("data", filterDoc)
                        )
                    )
                } else {
                    "" to listOf(D())
                }

//		println(Util.toPretty(query))
                Flux
                    .from(MongodbUtil.getCollection(coll)
                        .aggregate(query))
                    .map {
//						println("Doc => $it")
                        it
                    }
                    .collectList()
                    .map {
//						println("Cnt => ${it.count()}")
                        Status.status200Ok(it)
                    }
            }
    } //  end of count()

    /**
     * {"status":200,"message":"OK",
     *   "data":[
     *     {"hx":0.96,"hy":0.17,"count":1},
     *     {"hx":0.92,"hy":0.11,"count":1}
     *   ],"field_list":[]
     * }
     */
    fun getFirstLastTapHeatmap(req: SafeRequest): Mono<Map<String, Any>> {
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val viewIdList = req.splitQueryOrDefault("view_id_list", "")
        val orientation = req.getQueryOrDefault("orientation", 1)

        val type = req.getQueryOrDefault("type", HeatmapServiceFirstLastTap.TYPE_FIRSTTAP)

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(type, "type").required(listOf(
                HeatmapServiceFirstLastTap.TYPE_FIRSTTAP,
                HeatmapServiceFirstLastTap.TYPE_LASTTAP))
            .new(orientation, "orientation").required(listOf(1, 2))

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())
        val heatmapServ = HeatmapServiceFirstLastTap()
        val mongoResStream = heatmapServ.getMongoResStreamFirstLastTap(
            appIdStream,
            appIdList, versionList, fromDate, toDate, viewIdList, orientation, type
        )
        val respStream = mongoResStream.map { result ->
            Status.status200Ok(result)
        }
        return respStream

    }


    fun getScrollViewHeatmap(req: SafeRequest): Mono<Map<String, Any>> {
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val viewIdList = req.splitQueryOrDefault("view_id_list", "")

        val type = req.getQueryOrDefault("type", HeatmapServiceActionType.TAP)

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(type, "type").required(
                listOf(
                    HeatmapServiceActionType.TAP,
                    HeatmapServiceActionType.DOUBLETAP,
                    HeatmapServiceActionType.LONGTAP,
                    HeatmapServiceActionType.SWIPE,
                    HeatmapServiceActionType.RESPONSE,
                    HeatmapServiceActionType.NORESPONSE,
                )
            )

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())
        val heatmapServ = HeatmapServiceScrollView()
        val mongoResStream = heatmapServ.getMongoResStreamScrollView(
            appIdStream,
            appIdList, versionList, fromDate, toDate, viewIdList,
            HeatmapServiceScrollView.TYPE_SCROLLVIEW,
            type
        )
        val respStream = mongoResStream.map { result ->
            Status.status200Ok(result)
        }
        return respStream

    }

    fun getReachRateHeatmap(req: SafeRequest): Mono<Map<String, Any>> {
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val viewIdList = req.splitQueryOrDefault("view_id_list", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())
        val heatmapServ = HeatmapServiceScrollView()
        val mongoResStream = heatmapServ.getMongoResStreamScrollView(
            appIdStream,
            appIdList, versionList, fromDate, toDate, viewIdList,
            HeatmapServiceScrollView.TYPE_REACHRATE
        )
        val respStream = mongoResStream.map { result ->
            Status.status200Ok(result)
        }
        return respStream

    }
}