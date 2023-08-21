package io.userhabit.polaris.service

import io.userhabit.batch.indicators.*
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.common.utils.ViewPeasant
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P

object DwellTimeService {
    private val log = Loggers.getLogger(DwellTimeService::class.java)
    // @deprecated
    // lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>

    /**
     * @comment 21.11.18 yj
     * @sample [GET {{localhost}}/v3/dwell_time/{ids}/count?app_id_list=e6c101f020e1018b5ba17cdbe32ade2d679b44bc&from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z]
     * @return data=[...]
     * {
    "dwell_time": 1215523890000
     * }
     * @see [https://github.com/userhabit/uh-issues/issues/388]
     */
    fun count(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val by = req.getQueryOrDefault("by", "all")
        val duration = req.getQueryOrDefault("duration", "all")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)
        val fieldList = req.splitQueryOrDefault("field_list", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(duration, "duration").required(listOf("month", "week", "day", "all"))
            .new(limit, "limit").max(100)

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val groupDoc = if (duration == "day") {
            D("\$dateToString", D("format", "%Y-%m-%dT00:00:00.000Z").append("date", "\$stz"))
        } else if (duration == "week") {
            D(
                "\$toString",
                D(
                    "\$dateFromString",
                    D("format", "%G-%V").append(
                        "dateString",
                        D("\$dateToString", D("format", "%G-%V").append("date", "\$stz"))
                    )
                )
            )
        } else if (duration == "month") {
            D("\$dateToString", D("format", "%Y-%m-01T00:00:00.000Z").append("date", "\$stz"))
        } else { // duration to all
            D()
        }

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }

                val coll = IndicatorAllByApp.COLLECTION_NAME_PT24H

                val matchQuery = D()
                    .append(
                        P.stz,
                        D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
                    )
                    .append(P.ai, D("\$in", QueryPeasant.convertToObjectIdList(appIdList)))

                if (versionList.isNotEmpty()) {
                    matchQuery.append(P.av, D("\$in", versionList))
                }

                val projectDoc = D().append("_id", 0).append(P.stzK, "\$_id.${P.stz}").append(P.dtK, "\$${P.dt}")

                val query = listOf(
                    D("\$match", matchQuery),

                    D(
                        "\$group", D()
                            .append(
                                "_id", D()
                                    .append(P.stz, groupDoc)
                            )
                            .append(P.dt, D("\$sum", "\$${P.dt}"))
                    ),
                    D("\$project", projectDoc),
                    D("\$skip", skip),
                    D("\$limit", limit)
                )

                Flux
                    .from(
                        MongodbUtil.getCollection(coll)
                            .aggregate(query)
                    )
                    .collectList()
                    .doOnError {
                        log.info(Util.toPretty(query))
                    }
                    .map {
                        Status.status200Ok(it)
                    }

            }
    }

    /**
     * @author cjh
     * @sample GET {{localhost}}/v3/dwell_time/{ids}/rank?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z
     * @param remove_session_start_end_view: ###SESSION_START###, ###SESSION_END###를 결과에서 지우는 파라미터
     * @return data=[...]
     * {
        "dwell_time": 0.0,
        "view_hash_id": 1136506182,
        "rank": 1,
        "view": {
        "_id": {
            "timestamp": 1636610884,
            "date": 1636610884000
        },
        "ai": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
        "av": "3.6.1",
        "vhi": 1136506182,
        "alias": null,
        "favorite": false,
        "vi": "com.hdsec.android.mainlib.SmartActivitySI310001",
        "app_version": [
            3,
            6,
            1
        ]
     * }
     * },
     */
    fun rank(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val by = req.getQueryOrDefault("by", "view")
        val viewIdList = req.splitQueryOrDefault("view_id_list", "")
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)
        val fieldList = req.splitQueryOrDefault("field_list", "")
        val sessionStartAndEndFlag = req.getQueryOrDefault("remove_session_start_end_view", "false").toBoolean()

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(by, "by").required(listOf("view"))

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()
        val filterDoc =
            if (viewIdList.isNotEmpty()) D("\$in", listOf("\$${P.vhiK}", viewIdList.map { it.toLong() })) else D()

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }

                val coll: String
                val query: List<D>

                if (by == "view") {
                    coll = IndicatorAllByView.COLLECTION_NAME_PT24H

                    val matchQuery =
                        this._getMatchPipe(fromDate, toDate, QueryPeasant.convertToObjectIdList(appIdList), versionList, sessionStartAndEndFlag)


                    query = listOf(
                        D("\$match", matchQuery),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.vhi, "\$${P.vhi}")
                                )
                                .append(P.dt, D("\$sum", "\$${P.dt}"))
                                .append(P.vco, D("\$sum", "\$${P.vco}"))
                        ),

                        D(
                            "\$project", D(
                                P.dt, D(
                                    "\$divide", listOf(
                                        D("\$cond", listOf(D("\$eq", listOf("\$${P.vco}", 0)), 0, "\$${P.dt}")),
                                        D("\$cond", listOf(D("\$eq", listOf("\$${P.vco}", 0)), 1, "\$${P.vco}"))
                                    )
                                )
                            )
                        ),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.dt, "\$${P.dt}")
                                )
                                .append(
                                    "arr", D()
                                        .append("\$push", D(P.vhi, "\$_id.${P.vhi}"))
                                )
                        ),

                        D("\$sort", D("_id.${P.dt}", -1)),

                        D(
                            "\$group", D()
                                .append("_id", 1)
                                .append(
                                    "arr", D()
                                        .append("\$push", D(P.dt, "\$_id.${P.dt}").append("arr", "\$arr"))
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
                                .append(P.dtK, "\$arr.${P.dt}")
                                .append(P.vhiK, "\$arr.arr.${P.vhi}")
                                .append("rank", D("\$add", listOf("\$rank", 1)))
                        ),

                        D(
                            "\$lookup", D()
                                .append("from", ViewList.COLLECTION_NAME_PT24H)
                                .append("let", D(P.vhi, "\$${P.vhiK}").append(P.ai, "\$${P.aiK}"))
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

                        D("\$skip", skip),
                        D("\$limit", limit)
                    )
                } else {
                    coll = ""
                    query = listOf(D())
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