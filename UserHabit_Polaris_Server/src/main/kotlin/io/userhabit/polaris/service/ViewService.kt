package io.userhabit.polaris.service

import io.userhabit.batch.indicators.IndicatorAllByApp
import io.userhabit.batch.indicators.IndicatorAllByView
import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.common.utils.ViewPeasant
import io.userhabit.polaris.service.flow.FlowServiceGetGraph
import io.userhabit.polaris.service.session.SessionServiceSessCount
import io.userhabit.polaris.service.view.ViewServiceViewCount
import io.userhabit.polaris.service.view.ViewServiceViewList
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P

object ViewService {
    private val log = Loggers.getLogger(this.javaClass)
    // @deprecated
    // lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>

    /**
     * 스크린 정보
     * @author cjh
     * @comment 21.11.18 yj
     * @sample GET {{localhost}}/v3/view/{ids}
     * @param [version_list,skip,limit,count,sort_field,sort_value]
     * @return data=[{
    "_id": "305666159",
    "av": "3.6.1",
    "vi": "com.hdsec.android.mainlib.SmartActivitySM010003",
    "alias": "aliastest",
    "favorite": false,
    "path": "S3://test/test"
     * }, ... ]
     * @see: Git Issue(#375, #667)
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val viewIdList = req.splitParamOrDefault("ids", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val sortField = req.getQueryOrDefault("sort_field", "${P.avK}")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)
        val count = req.getQueryOrDefault("count", "false").toBoolean()

        val fieldList = req.splitQueryOrDefault("field_list", "")


        val vali = Validator()
            .new(limit, "limit").max(100)

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList -> // it = [ elem1, elem2 ]
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }

                val matchQuery = D()
                    .append(P.ai, D("\$in", QueryPeasant.convertToObjectIdList(appIdList)))

                if (versionList.isNotEmpty()) {
                    matchQuery.append(P.av, D("\$in", versionList))
                }

                if (viewIdList.isNotEmpty() && viewIdList.first() != "*") {
                    matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
                }
                val (coll, query) = ViewList.COLLECTION_NAME_PT24H to mutableListOf<D>(
                    D("\$match", matchQuery),

                    D(
                        "\$group", D()
                            .append(
                                "_id", D()
                                    .append(P.vhi, "\$${P.vhi}")
                            )
                            .append(
                                "data", D(
                                    "\$push", D()
                                        .append(P.av, "\$${P.av}")
                                        .append(P.vi, "\$${P.vi}")
                                        .append("alias", "\$alias")
                                        .append("path", "\$path")
                                )
                            )
                    ),

                    D(
                        "\$project", D() // 최신 뷰만 다음 파이프라인으로 전달
                            .append("_id", 1) // Todo: _id: 0 to _id: 1
                            .append("data", D("\$arrayElemAt", listOf("\$data", 0)))
                    ),

                    D("\$project", fieldList.fold(D()) { acc, it -> acc.append(it, 1) }
                        .let {
                            it.append("_id", D("\$toString", "\$_id.vhi")) // Todo: _id to _id.vhi
                            if (it.containsKey("created_date")) it.append(
                                "created_date",
                                D("\$dateToString", D("date", "\$created_date").append("format", "%Y-%m-%dT%H:%M:%SZ"))
                            )
                            if (it.containsKey("updated_date")) it.append(
                                "updated_date",
                                D("\$dateToString", D("date", "\$updated_date").append("format", "%Y-%m-%dT%H:%M:%SZ"))
                            )
                            // #667 Issue
                            it.append("data", "\$data")
                        }
                    ),
                    /**
                     * @see [https://github.com/userhabit/uh-issues/issues/375]
                     */
                    D(mapOf("\$replaceWith" to mapOf("\$mergeObjects" to listOf(mapOf("_id" to "\$_id"), "\$data")))),
                    D("\$sort", D(P.getAbbreviation(sortField), sortValue)),
                    D("\$skip", skip),
                    D("\$limit", limit)

                )
                if (count) {
                    query.remove(D("\$limit", limit))
                    query.add(D("\$count", "totalDocs"))
                }
//				println(Util.toPretty(query))

                Flux
                    .from(MongodbUtil.getCollection(coll).aggregate(query))
                    .collectList()
                    .map {
//						println("=> ${Util.toPretty(it)}")
                        Status.status200Ok(it)
                    }
                // end of getAppIdList()
            }
        // end of get()
    }

    /**
     * 스크린 수
     * @author lje
     * @comment 21.11.18 yj
     * @sample GET {{localhost}}/v3/view/{ids}/count?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z
     * @return data=[
     * {
    "session_time": "2021-11-12T00:00:00Z",
    "view_count": 12224
     * }, ... ]
     */
    fun count(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val sortField = req.getQueryOrDefault("sort_field", "_id")
        val sortValue = req.getQueryOrDefault("sort_value", 1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)

        val duration = req.getQueryOrDefault("duration", "day")
        val by = req.getQueryOrDefault("by", "session")
        val isUnique = req.getQueryOrDefault("is_unique", "false").toBoolean()
        val fieldList = req.splitQueryOrDefault("field_list", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(limit, "limit").max(100)
            .new(duration, "duration").required(listOf("all", "day", "week", "month"))
            .new(by, "by").required(listOf("all", "session"))

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

                /**
                 * case 1. all (admin mayby ???)
                 * case 2. sesion + unique
                 * case 3. sesion + no_unique
                 */

                val (coll, query) = if (by == "all") {
                    /**
                     * @author TODO
                     * 아마도 admin에서???
                     */
                    throw PolarisException.status400BadRequest("by=all is bad parameter.", 401)
                    "" to listOf(D())
                } else if (by == "session") {
                    /**
                     *  스크린뷰 / 유니크뷰
                     *  @author lje
                     */
                    val group: D
                    val projection: D
                    val groupKey = if (isUnique) P.uvco else P.vco

                    val matchQuery = D()
                        .append(
                            P.stz,
                            D("\$gte", Date.from(Instant.parse(fromDate))).append(
                                "\$lte",
                                Date.from(Instant.parse(toDate))
                            )
                        )
                        .append(P.ai, D("\$in", appIdInObjIds))

                    if (versionList.isNotEmpty()) {
                        matchQuery.append(P.av, D("\$in", versionList))
                    }

                    val groupDoc = if (duration == "day") {
                        D("\$dateToString", D("format", "%Y-%m-%dT00:00:00.000Z").append("date", "\$${P.stz}"))
                    } else if (duration == "week") {
                        D(
                            "\$toString",
                            D(
                                "\$dateFromString",
                                D("format", "%G-%V").append(
                                    "dateString",
                                    D("\$dateToString", D("format", "%G-%V").append("date", "\$${P.stz}"))
                                )
                            )
                        )
                    } else if (duration == "month") {
                        D("\$dateToString", D("format", "%Y-%m-01T00:00:00.000Z").append("date", "\$${P.stz}"))
                    } else { // all
                        D()
                    }

                    if (isUnique) {
                        projection = D().append("_id", 0)
                            .append(P.stzK, "\$_id.${P.stz}")
                            .append(P.uvcoK, "\$${P.uvco}")
                    } else {
                        projection = D().append("_id", 0)
                            .append(P.stzK, "\$_id.${P.stz}")
                            .append(P.vcoK, "\$${P.vco}")
                    }

                    val common = D("\$match", matchQuery)

                    group = D(
                        "\$group", D()
                            .append(
                                "_id", D()
                                    .append(P.stz, groupDoc)
                            )
                            .append(groupKey, D("\$sum", "\$${groupKey}"))
                    )


                    IndicatorAllByApp.COLLECTION_NAME_PT24H to listOf(
                        common,
                        group,
                        D("\$project", projection),
                        D("\$skip", skip),
                        D("\$limit", limit)
                    )
                } else {
                    throw PolarisException.status400BadRequest("Error", 500)
                    "" to listOf(D())
                }

//				println(Util.toPretty(query))
                Flux
                    .from(
                        MongodbUtil.getCollection(coll)
                            .aggregate(query)
                    )
                    .collectList()
                    .map {
                        Status.status200Ok(it)
                    }
            }
    } // end of count()

    /**
     * @author cjh
     * @comment 21.11.18 yj
     * @sample
     *  GET {{localhost}}/v3/view/{ids}/rank?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z
     * @param
     *  remove_session_start_end_view: ###SESSION_START###, ###SESSION_END###를 결과에서 지우는 파라미터
     *  sort_field: rank(Default), view_count, view_hash_id을 기준으로 정렬
     * @return
     * data=[{
     *  "view_count": 20775,
     *  "view_hash_id": 306589677,
     *  "rank": 1,
     *  "view": {
     *      "_id": {
     *          "timestamp": 1636610884,
     *          "date": 1636610884000
     *      },
     *      "ai": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
     *      "av": "3.6.1",
     *      "vhi": 306589677,
     *      "alias": null,
     *      "favorite": false,
     *      "vi": "com.hdsec.android.mainlib.SmartActivitySM020000",
     *      "app_version": [
     *          3,
     *          6,
     *          1
     *      ]
     *  }
     * }, ... ]
     */
    fun rank(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitParamOrDefault("ids")
//		val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val sortField = req.getQueryOrDefault("sort_field", "rank")
        val sortValue = req.getQueryOrDefault("sort_value", 1)
        val viewIdList = req.splitQueryOrDefault("view_id_list", "")
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)
        val by = req.getQueryOrDefault("by", "view")
        val sessionStartAndEndFlag = req.getQueryOrDefault("remove_session_start_end_view", "false").toBoolean()

        val fieldList = req.splitQueryOrDefault("field_list", "")

        val vali = Validator()
            .new(appIdList, "ids").required()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(by, "by").required(listOf("view"))
            .new(sortField, "sort_field").required(listOf("rank", "view_count", "view_hash_id"))
            .new(sortValue, "sort_value").required(listOf(1, -1))

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()
        val filterDoc =
            if (viewIdList.isNotEmpty()) D("\$in", listOf("\$${P.vhiK}", viewIdList.map { it.toLong() })) else D()
        // lookup 하위 pipeline $match $expr 사용 시에 $nin, $regex 사용 불가

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("ids", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

                val (coll, query) = if (by == "view") {
                    val matchQuery =
                        this._getMatchPipe(fromDate, toDate, appIdInObjIds, versionList, sessionStartAndEndFlag)

                    IndicatorAllByView.COLLECTION_NAME_PT24H to listOf(
                        D("\$match", matchQuery),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.vhi, "\$${P.vhi}")
                                )
                                .append(P.vco, D("\$sum", "\$${P.vco}"))
                        ),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.vco, "\$${P.vco}")
                                )
                                .append(
                                    "arr", D()
                                        .append("\$push", D(P.vhi, "\$_id.${P.vhi}"))
                                )
                        ),

                        D("\$sort", D("_id.${P.vco}", -1)),

                        D(
                            "\$group", D()
                                .append("_id", 1)
                                .append(
                                    "arr", D()
                                        .append("\$push", D(P.vco, "\$_id.${P.vco}").append("arr", "\$arr"))
                                )
                        ),

                        D(
                            "\$unwind", D()
                                .append("path", "\$arr")
                                .append("includeArrayIndex", "rank")
                        ),

                        D("\$unwind", "\$arr.arr"),

                        D(
                            "\$project", D()
                                .append("_id", 0)
                                .append(P.vcoK, "\$arr.${P.vco}")
                                .append(P.vhiK, "\$arr.arr.${P.vhi}")
                                .append("rank", D("\$add", listOf("\$rank", 1)))
                        ),

                        D(
                            "\$lookup", D()
                                .append("from", ViewList.COLLECTION_NAME_PT24H)
                                .append("let", D(P.vhi, "\$${P.vhiK}"))
                                .append(
                                    "pipeline", listOf(
                                        D(
                                            "\$match", D(
                                                "\$expr", D(
                                                    "\$and", listOf(
                                                        D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                                        versionDoc
                                                    )
                                                )
                                            )
                                        ),
                                        D(
                                            "\$addFields", D(
                                                P.avK, D(
                                                    "\$map",
                                                    D("input", D("\$split", listOf("\$${P.av}", ".")))
                                                        .append("as", "t")
                                                        .append("in", D("\$toInt", "\$\$t"))
                                                )
                                            )
                                        ),
                                        D(
                                            "\$sort",
                                            D("${P.avK}.0", -1).append("${P.avK}.1", -1).append("${P.avK}.2", -1)
                                                .append("${P.avK}.3", -1)
                                        ),
                                        D("\$limit", 1)
                                    )
                                )
                                .append("as", "view")
                        ),

                        D("\$unwind", "\$view"),

                        D(
                            "\$match", D(
                                "\$expr", D(
                                    "\$and", listOf(
                                        filterDoc
                                    )
                                )
                            )
                        ),
//                        .sort(D().let { doc ->
//                        if (sortField != "time_stamp") doc.append(
//                            P.getAbbreviation(sortField),
//                            sortValue
//                        ) else doc.append("_id." + P.getAbbreviation(sortField), sortValue)
//                    })
                        D(
                            "\$sort", mapOf(
                                sortField to sortValue
                            )
                        ),
                        D("\$skip", skip),
                        D("\$limit", limit)
                    )
                } else {
                    // Todo : By View 이외 분기가 작성 되지 않음.
                    "" to listOf(D())
                }

//		println(Util.toPretty(query))

                Flux
                    .from(
                        MongodbUtil.getCollection(coll)
                            .aggregate(query)
                    )
                    .collectList()
                    .map {
                        Status.status200Ok(it)
                    }
            }
        // end of rank()
    }

    /**
     * {
     *   "status": 200,
     *   "message": "OK",
     *   "data": [{view_count: 52}]
     * }
     */
    fun getTotalCount(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")

        val vali = Validator()
            .new(appIdList, "app_id_list").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val vcountService = ViewServiceViewCount()
        val mongoResStream = vcountService.getMongoResStreamTotalCount(
            appIdStream, appIdList, versionList
        )
        val respStream = mongoResStream.map { result ->
            Status.status200Ok(result)
        }
        return respStream

    }

    /**
     * @response
     * {
     *   "status": 200,
     *   "message": "OK",
     *   "data": [
     *     {
     *         "vhi": 682035254,
     *         "count": 180,
     *         "view": [
     *             {
     *                 "ai": {
     *                     "timestamp": 0,
     *                     "counter": 4,
     *                     "randomValue1": 0,
     *                     "randomValue2": 0
     *                 },
     *                 "av": "1.0.1",
     *                 "vhi": 682035254,
     *                 "path": "s3://service-uh-polaris-stage-2/attach/000000000000000000000004/1.0.1/ec7a20286ad66f8ca3af6a7e2327188e",
     *                 "vi": "(IntroActivity)",
     *                 "alias": "(IntroActivity)"
     *             }
     *         ]
     *     },
     *     ...
     *   ]
     * }
     */
    fun getViewList(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val sortField = req.getQueryOrDefault("sort_field", ViewServiceViewList.SORTKEY_SESSION)
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 100)

        val vali = Validator()
            .new(appIdList, "app_id_list").required()
            .new(sortField, "sort_field").required(
                listOf(
                    ViewServiceViewList.SORTKEY_SESSION,
                    ViewServiceViewList.SORTKEY_SCREENVIEW,
                    ViewServiceViewList.SORTKEY_SCREENVIEWTIME,
                    ViewServiceViewList.SORTKEY_SCREENVIEWACTION,
                    ViewServiceViewList.SORTKEY_SCREENVIEWACTIONNORESP,
                    ViewServiceViewList.SORTKEY_DAU,
                    ViewServiceViewList.SORTKEY_CRASH,
                    ViewServiceViewList.SORTKEY_APPEND,
                )
            )
            .new(limit, "limit").max(100)

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val vcountService = ViewServiceViewList()
        val mongoResStream = vcountService.getMongoResStreamViewList(
            appIdStream, appIdList, versionList, sortField, sortValue, skip, limit
        )
        val respStream = mongoResStream.map { result ->
            Status.status200Ok(result)
        }
        return respStream

    }

    /**
     * @response
     * {
     *   "status": 200,
     *   "message": "OK",
     *   "data": [{"view_count":48}]
     * }
     */
    fun getViewCountOfList(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val sortField = req.getQueryOrDefault("sort_field", ViewServiceViewList.SORTKEY_SESSION)

        val vali = Validator()
            .new(appIdList, "app_id_list").required()
            .new(sortField, "sort_field").required(
                listOf(
                    ViewServiceViewList.SORTKEY_SESSION,
                    ViewServiceViewList.SORTKEY_SCREENVIEW,
                    ViewServiceViewList.SORTKEY_SCREENVIEWTIME,
                    ViewServiceViewList.SORTKEY_SCREENVIEWACTION,
                    ViewServiceViewList.SORTKEY_SCREENVIEWACTIONNORESP,
                    ViewServiceViewList.SORTKEY_DAU,
                    ViewServiceViewList.SORTKEY_CRASH,
                    ViewServiceViewList.SORTKEY_APPEND,
                )
            )

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val vcountService = ViewServiceViewList()
        val mongoResStream = vcountService.getMongoResStreamViewCount(
            appIdStream, appIdList, versionList, sortField
        )
        val respStream = mongoResStream.map { result ->
            Status.status200Ok(result)
        }
        return respStream

    }

    fun _getMatchPipe(
        fromDate: String,
        toDate: String,
        appIdInObjIds: List<ObjectId>,
        versionList: List<String>,
        sessionStartAndEndFlag: Boolean
    ): D {
        val matchQuery = D()
            .append(
                P.stz,
                D("\$gte", Date.from(Instant.parse(fromDate))).append(
                    "\$lte",
                    Date.from(Instant.parse(toDate))
                )
            )
            .append(P.ai, D("\$in", appIdInObjIds))


        if (sessionStartAndEndFlag) {
            matchQuery.append(
                P.vhi,
                D("\$nin", listOf(ViewPeasant.getVhiOfSessionStart(), ViewPeasant.getVhiOfSessionEnd()))
            )
        }

        if (versionList.isNotEmpty()) {
            matchQuery.append(P.av, D("\$in", versionList))
        }

        return matchQuery
    }
}
