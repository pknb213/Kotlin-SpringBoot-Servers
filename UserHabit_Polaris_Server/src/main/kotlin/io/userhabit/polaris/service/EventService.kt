package io.userhabit.polaris.service

import io.userhabit.batch.indicators.IndicatorEventByView
import io.userhabit.batch.indicators.IndicatorAllByView
import io.userhabit.batch.indicators.IndicatorEventByApp
import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.common.utils.ViewPeasant
import io.userhabit.polaris.service.event.EventServiceFind
import org.bson.types.ObjectId
import io.userhabit.polaris.EventType as ET
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P

/**
 * @author sbnoh
 */
object EventService {
    private val log = Loggers.getLogger(this.javaClass)
    val COLLECTION_NAME = "event"
    // @deprecated
    // lateinit var FIELD_LIST: List<Map<String, String>>

    /**
     * cRud 읽기
     * field_list query (options) : ,(콤마) 구분자로 된 컬랙션 필드 이름들
     * sort_k query (options) : 기본 _id
     * sort_v query (options) : 기본 -1
     * skip query (options) : 기본 0
     * limit query (options) : 기본 20, 최대 100
     *
     * @comment 21.11.18 yj
     * @sample GET {{localhost}}/v3/event/{ids}
     * @param
     *      search_expr = {}
     * @return data: [
     * {
     *   "event_type": 8800,
     *   "session_id": "0_948b3a3829cc4a078ed5e72090931cbe",
     *   "time_stamp": 452967000
     * }, ... ]
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids")

        val fieldList = req.splitQueryOrDefault("field_list")
        val sortField = req.getQueryOrDefault("sort_field", "time_stamp")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 20)
        val searchExpr = req.getQueryOrDefault("search_expr")

        val validator = Validator()
            .new(limit, "limit").max(100)

        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        val isSystem = Level.hasPermission(req.getLevel(), Level.SYSTEM)

        return Flux
            .from(
                if (isSystem)
                    Flux.just()
                else
                    MongodbUtil.getCollection(SessionService.COLLECTION_NAME) // TODO lru cache 로 만들어야 할까?
                        .find(D(P.ai, D("\$in", QueryPeasant.convertToObjectIdList(req.getAppIdList()))))
                        .projection(D("_id", 0).append(P.si, "\$_id.${P.si}"))
            )
            .collectList()
            .flatMap { sil ->
                Flux
                    .from(
                        MongodbUtil.getCollection(COLLECTION_NAME)
                            .find(D().let { doc ->
                                if (searchExpr.isNotEmpty()) doc.putAll(D.parse(searchExpr))// 이 로직은 반드시 아래 조건 추가 로직보다 우선되어야 함
                                if (!isSystem) doc.append("_id.${P.si}", D("\$in", sil.map { it[P.si] }))
                                if (ids.first() != "*") doc.append("_id.${P.si}", D("\$in", ids))
                                doc.keys.forEach { doc[P.getAbbreviation(it)] = doc.remove(it) }
                                doc
                            })
                            .sort(D().let { doc ->
                                if (sortField != "time_stamp") doc.append(
                                    P.getAbbreviation(sortField),
                                    sortValue
                                ) else doc.append("_id." + P.getAbbreviation(sortField), sortValue)
                            })
                            .skip(skip)
                            .limit(limit)
                            .projection(fieldList
                                .fold(D()) { doc, it -> doc.append(it, "\$${P.getAbbreviation(it)}") }
                                .let {
                                    if (it.containsKey(P.siK)) it.append(P.siK, "\$_id.${P.si}")
                                    if (it.containsKey(P.tsK)) it.append(P.tsK, "\$_id.${P.ts}")
                                    it
                                }
                            )
                    )
                    .collectList()
                    .map {
                        Status.status200Ok(it)
                    }
            }
    }

    /**
     * cruD 삭제
     * ids (mandatory) : ,(콤마) 구분자로 된 Session ID(String value)
     */
    fun delete(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids", "")

        val delete = MongodbUtil.getCollection(COLLECTION_NAME)
            .deleteMany(D("_id.${P.si}", D("\$in", ids)))

        return Mono.from(delete)
            .map {
                Status.status200Ok(ids.toString())
            }
    }

    /**
     *  이벤트 카운트
     *  @author lje
     *  @comment 21.11.18 yj
     *  request: GET {{localhost}}/v3/event/{ids}/count?from_date=2021-10-01T00:00:00Z&to_date=2021-12-31T00:00:00Z&duration=day
     *  response: data: [...]
     * {
     *   "app_id": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
     *   "session_time": "2021-11-05T00:00:00Z",
     *   "event_count": 160050
     * }, ...
     */
    fun count(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val viewIdList = req.splitQueryOrDefault("view_id_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val sortField = req.getQueryOrDefault("sort_field", "_id")
        val sortValue = req.getQueryOrDefault("sort_value", 1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)

        val duration = req.getQueryOrDefault("duration", "all")
        val by = req.getQueryOrDefault("by", "all")
        val type = req.getQueryOrDefault("type", "all")
        val fieldList = req.splitQueryOrDefault("field_list", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(duration, "duration").required(listOf("all", "day", "week", "month"))
            .new(type, "type")
            .required(listOf("all", "no_response", "swipe", "long_tap", "double_tap", "tap", "swipe_direction"))
            .new(by, "by").required(listOf("all", "view"))
            .new(limit, "limit").max(100)

        if (by == "view") vali.new(viewIdList, "view_id_list").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

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

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

                val (coll, query) = if (by == "all") {

                    val projectDoc = D().append("_id", 0)
                        .append(P.aiK, "\$_id.${P.ai}")
                        .append(P.stzK, "\$_id.${P.stz}")
                        .append(P.ecoK, "\$${P.eco}")

                    val typeDoc = when (type) {
                        "no_response" -> D("\$eq", ET.NOACT_TAP)
                        "tap" -> D("\$eq", ET.REACT_TAP)
                        "double_tap" -> D("\$eq", ET.REACT_DOUBLE_TAP)
                        "long_tap" -> D("\$eq", ET.REACT_LONG_TAP)
                        "swipe" -> D("\$eq", ET.REACT_SWIPE)
                        else -> D("\$in", listOf(ET.REACT_TAP, ET.REACT_DOUBLE_TAP, ET.REACT_LONG_TAP, ET.REACT_SWIPE))
                    }

                    val matchQuery = D()
                        .append(
                            P.stz,
                            D("\$gte", Date.from(Instant.parse(fromDate))).append(
                                "\$lte",
                                Date.from(Instant.parse(toDate))
                            )
                        )
                        .append(P.ai, D("\$in", appIdInObjIds))
                        .append(P.t, typeDoc)

                    if (versionList.isNotEmpty()) {
                        matchQuery.append(P.av, D("\$in", versionList))
                    }

                    if (viewIdList.isNotEmpty()) {
                        matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
                    }

                    IndicatorEventByApp.COLLECTION_NAME_PT24H to listOf(
                        D("\$match", matchQuery),
                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.ai, "\$${P.ai}") //app list 에서 필요
                                        .append(P.stz, groupDoc)
                                )
                                .append(P.eco, D("\$sum", "\$${P.eco}"))
                        ),
                        D("\$project", projectDoc),
                        D("\$sort", D(sortField, sortValue)),
                        D("\$skip", skip),
                        D("\$limit", limit)
                    )
                } else if (by == "view") {
                    if (type == "swipe_direction") {
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

                        if (viewIdList.isNotEmpty()) {
                            matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
                        }

                        IndicatorAllByView.COLLECTION_NAME_PT24H to mutableListOf(
                            D("\$match", matchQuery),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.vhi, "\$${P.vhi}")
                                    )
                                    .append(P.dit, D("\$sum", "\$${P.dit}"))
                                    .append(P.dir, D("\$sum", "\$${P.dir}"))
                                    .append(P.dil, D("\$sum", "\$${P.dil}"))
                                    .append(P.dib, D("\$sum", "\$${P.dib}"))
                            ),

                            D(
                                "\$addFields", D()
                                    .append(
                                        "total",
                                        D("\$sum", listOf("\$${P.dit}", "\$${P.dir}", "\$${P.dil}", "\$${P.dib}"))
                                    )
                            ),

                            D(
                                "\$project", D()
                                    .append("_id", 0)
                                    .append(P.vhiK, "\$_id.${P.vhi}")
                                    .append(P.ditK, "$${P.dit}")
                                    .append(P.dirK, "$${P.dir}")
                                    .append(P.dilK, "$${P.dil}")
                                    .append(P.dibK, "$${P.dib}")
                                    .append(
                                        "dt_ratio",
                                        D(
                                            "\$cond",
                                            listOf(
                                                D("\$eq", listOf("\$${P.dit}", 0)),
                                                0,
                                                D(
                                                    "\$multiply",
                                                    listOf(D("\$divide", listOf("\$${P.dit}", "\$total")), 100)
                                                )
                                            )
                                        )
                                    )
                                    .append(
                                        "dr_ratio",
                                        D(
                                            "\$cond",
                                            listOf(
                                                D("\$eq", listOf("\$${P.dir}", 0)),
                                                0,
                                                D(
                                                    "\$multiply",
                                                    listOf(D("\$divide", listOf("\$${P.dir}", "\$total")), 100)
                                                )
                                            )
                                        )
                                    )
                                    .append(
                                        "dl_ratio",
                                        D(
                                            "\$cond",
                                            listOf(
                                                D("\$eq", listOf("\$${P.dil}", 0)),
                                                0,
                                                D(
                                                    "\$multiply",
                                                    listOf(D("\$divide", listOf("\$${P.dil}", "\$total")), 100)
                                                )
                                            )
                                        )
                                    )
                                    .append(
                                        "db_ratio",
                                        D(
                                            "\$cond",
                                            listOf(
                                                D("\$eq", listOf("\$${P.dib}", 0)),
                                                0,
                                                D(
                                                    "\$multiply",
                                                    listOf(D("\$divide", listOf("\$${P.dib}", "\$total")), 100)
                                                )
                                            )
                                        )
                                    )
                            )
                        )
                    } else {
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

                        if (viewIdList.isNotEmpty()) {
                            matchQuery.append(P.vhi, D("\$in", viewIdList.map { it.toLong() }))
                        }

                        IndicatorEventByView.COLLECTION_NAME_PT24H to mutableListOf(
                            D("\$match", matchQuery),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.vhi, "\$${P.vhi}")
                                            .append(P.t, "\$${P.t}")
                                    )
                                    .append(P.eco, D("\$sum", "\$${P.eco}"))
                            ),

                            D("\$sort", D("${P.eco}", -1)),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.vhi, "\$_id.${P.vhi}")
                                    )
                                    .append(
                                        "event", D(
                                            "\$push", D()
                                                .append(P.ecoK, "\$${P.eco}")
                                                .append(P.tK, "\$_id.${P.t}")
                                        )
                                    )
                            ),

                            D(
                                "\$project", D()
                                    .append("_id", 0)
                                    .append(P.vhiK, "\$_id.${P.vhi}")
                                    .append("event", 1)
                            )
                        )
                    }
                } else {
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
    }

    /**
     * @author cjh
     * @comment 21.11.18 yj
     * @sample GET {{localhost}}/v3/event/{ids}/rank?from_date=2022-01-01T00:00:00Z&to_date=2022-03-31T00:00:00Z&type=no_response
     * @param remove_session_start_end_view: ###SESSION_START###, ###SESSION_END###를 결과에서 지우는 파라미터
     * @return data: [{
     *
     * }]
     */
    fun rank(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val viewIdList = req.splitQueryOrDefault("view_id_list", "")
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)
        val by = req.getQueryOrDefault("by", "view")
        val type = req.getQueryOrDefault("type", "all")
        val fieldList = req.splitQueryOrDefault("field_list", "")
        val sessionStartAndEndFlag = req.getQueryOrDefault("remove_session_start_end_view", "false").toBoolean()

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(by, "by").required(listOf("view"))
            .new(type, "type").required(listOf("all", "no_response"))

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()
        val filterDoc =
            if (viewIdList.isNotEmpty()) D("\$in", listOf("\$${P.vhiK}", viewIdList.map { it.toLong() })) else D()

        var coll: String
        var query: List<D>

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

                if (by == "view") {
                    // 스크린 뷰당 이벤트 비율
                    if (type == "all") {
                        coll = IndicatorEventByView.COLLECTION_NAME_PT24H
                        val matchQuery =
                            this._getMatchPipe(fromDate, toDate, appIdInObjIds, versionList, sessionStartAndEndFlag)

                        query = listOf(
                            D("\$match", matchQuery),

                            D(
                                "\$lookup", D()
                                    .append("from", IndicatorAllByView.COLLECTION_NAME_PT24H)
                                    .append(
                                        "let",
                                        D().append(P.ai, "\$${P.ai}").append(P.vhi, "\$${P.vhi}")
                                            .append(P.av, "\$${P.av}").append(P.stz, "\$${P.stz}")
                                    )
                                    .append(
                                        "pipeline", listOf(
                                            D(
                                                "\$match", D(
                                                    "\$expr", D(
                                                        "\$and", listOf(
                                                            D("\$eq", listOf("\$${P.ai}", "\$\$${P.ai}")),
                                                            D("\$eq", listOf("\$${P.stz}", "\$\$${P.stz}")),
                                                            D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                                            D("\$eq", listOf("\$${P.av}", "\$\$${P.av}"))
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                    .append("as", "view")
                            ),

                            D("\$unwind", "\$view"),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.vhi, "\$${P.vhi}")
                                    )
                                    .append(P.eco, D("\$sum", "\$${P.eco}"))
                                    .append(P.vco, D("\$sum", "\$view.${P.vco}"))
                            ),

                            D(
                                "\$project", D(
                                    "value", D(
                                        "\$divide", listOf(
                                            D("\$cond", listOf(D("\$eq", listOf("\$${P.vco}", 0)), 0, "\$${P.eco}")),
                                            D("\$cond", listOf(D("\$eq", listOf("\$${P.vco}", 0)), 1, "\$${P.vco}"))
                                        )
                                    )
                                )
                            ),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append("value", "\$value")
                                    )
                                    .append(
                                        "arr", D()
                                            .append("\$push", D(P.vhi, "\$_id.${P.vhi}"))
                                    )
                            ),

                            D("\$sort", D("_id.value", -1)),

                            D(
                                "\$group", D()
                                    .append("_id", 1)
                                    .append(
                                        "arr", D()
                                            .append("\$push", D("value", "\$_id.value").append("arr", "\$arr"))
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
                                    .append("value", "\$arr.value")
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

                            D("\$skip", skip),
                            D("\$limit", limit)
                        )

                    }
                    // 무반응 탭 비율
                    else if (type == "no_response") {
                        coll = IndicatorEventByView.COLLECTION_NAME_PT24H

                        val matchQuery =
                            this._getMatchPipe(fromDate, toDate, appIdInObjIds, versionList, sessionStartAndEndFlag)

                        query = listOf(
                            D("\$match", matchQuery),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.vhi, "\$${P.vhi}")
                                    )
                                    .append("all_event", D("\$sum", "\$${P.eco}"))
                                    .append(
                                        "no_response_event",
                                        D(
                                            "\$sum",
                                            D(
                                                "\$cond",
                                                listOf(
                                                    D(
                                                        "\$in",
                                                        listOf(
                                                            "\$${P.t}",
                                                            listOf(
                                                                ET.NOACT_TAP,
                                                                ET.NOACT_DOUBLE_TAP,
                                                                ET.NOACT_LONG_TAP,
                                                                ET.NOACT_SWIPE
                                                            )
                                                        )
                                                    ), "\$${P.eco}", 0
                                                )
                                            )
                                        )
                                    )
                            ),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(
                                                "value", D(
                                                    "\$multiply", listOf(
                                                        D(
                                                            "\$divide", listOf(
                                                                D(
                                                                    "\$cond",
                                                                    listOf(
                                                                        D("\$eq", listOf("\$no_response_event", 0)),
                                                                        0,
                                                                        "\$no_response_event"
                                                                    )
                                                                ),
                                                                D(
                                                                    "\$cond",
                                                                    listOf(
                                                                        D("\$eq", listOf("\$no_response_event", 0)),
                                                                        1,
                                                                        "\$all_event"
                                                                    )
                                                                )
                                                            )
                                                        ),
                                                        100
                                                    )
                                                )
                                            )
                                    )
                                    .append(
                                        "arr", D()
                                            .append("\$push", D(P.vhi, "\$_id.${P.vhi}"))
                                    )
                            ),

                            D("\$sort", D("_id.value", -1)),

                            D(
                                "\$group", D()
                                    .append("_id", 1)
                                    .append(
                                        "arr", D()
                                            .append("\$push", D("value", "\$_id.value").append("arr", "\$arr"))
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
                                    .append("value", "\$arr.value")
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
//						println(it)
                        Status.status200Ok(it)
                    }
            }
        // end of rank()
    }

    fun getEventsOfSession(req: SafeRequest): Mono<Map<String, Any>> {

        val si = req.getQueryOrDefault("session_id", "")
        val sortField = req.getQueryOrDefault("sort_field", "time_stamp")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", -1)
        val appIdList = req.splitQueryOrDefault("app_id_list", "")

        val validator = Validator()
            // .new(limit, "limit").max(1000)
            .new(appIdList, "app_id_list").required()
            .new(si, "session_id").required()

        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())
        return appIdStream.flatMap { ownedAppIdList ->
            val esFind = EventServiceFind()
            esFind.getMongoResStreamEventsOfSession(ownedAppIdList, appIdList, si, skip, limit)
        }.map {
            Status.status200Ok(it)
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
                D("\$gte", Date.from(Instant.parse(fromDate)))
                    .append(
                        "\$lte", Date.from(Instant.parse(toDate))
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
