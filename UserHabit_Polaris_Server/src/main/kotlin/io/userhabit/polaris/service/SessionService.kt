package io.userhabit.polaris.service

import io.userhabit.batch.indicators.*
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.common.utils.ViewPeasant
import io.userhabit.polaris.service.session.SessionServiceSessCount
import io.userhabit.polaris.service.session.SessionServiceSessionInfo
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
object SessionService {
    private val log = Loggers.getLogger(this.javaClass)
    val COLLECTION_NAME = "session"
    // @deprecated
    // lateinit var FIELD_LIST: List<Map<String, String>>

    /**
     * cRud 읽기
     *
     * ids param (options) : ,(콤마) 구분자로 된 String value 없으면 저장된 모든 데이터
     * field_list query (options) : 기본 _id,si,di
     * sort_k query (options) : 기본 _id
     * sort_v query (options) : 기본 1
     * skip query (options) : 기본 0
     * limit query (options) : 기본 20, max 100
     *
     * @comment 21.11.18 yj
     * @param : GET {{localhost}}/v3/session/{ids}
     * @return
     *  data: [
     *      {
     *          "_id": "81c10070f1db795749f82fe17a6606de8a109302",
     *          "di": "2284d9643d48350cfdfc8a1e269794dc9e1e1ff8"
     *      },
     *      {
     *          "_id": "81e4f82f5e05d023eee64b3d44282a42706b2e27",
     *          "di": "65293252d913bec35844ae8420bcfc384fad57a7"
     *      },  ...
     * ]
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids")

        val fieldList = req.splitQueryOrDefault("field_list", "_id,aa,${P.di}")
        val sortField = req.getQueryOrDefault("sort_field", "_id")
        val sortValue = req.getQueryOrDefault("sort_value", 1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 99)
        val searchExpr = req.getQueryOrDefault("search_expr")

        val validator = Validator()
            .new(limit, "limit").max(100)

        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        return Flux
            .from(MongodbUtil.getCollection(COLLECTION_NAME)
                .find(D().let { doc ->
                    if (searchExpr.isNotEmpty()) doc.putAll(D.parse(searchExpr))// 이 로직은 반드시 아래 조건 추가 로직보다 우선되어야 함
                    if (!Level.hasPermission(req.getLevel(), Level.SYSTEM)) doc.append(
                        P.ai,
                        D("\$in", QueryPeasant.convertToObjectIdList(req.getAppIdList()))
                    )
                    if (ids.first() != "*") doc.append("_id", D("\$in", ids))
                    doc.keys.forEach { doc[P.getAbbreviation(it)] = doc.remove(it) }
                    doc
                })
                .sort(D(P.getAbbreviation(sortField), sortValue))
                .skip(skip)
                .limit(limit)
                .projection(fieldList
                    .fold(D()) { doc, it -> doc.append(it, "\$${P.getAbbreviation(it)}") }
                    .let {
                        it
                    }
                )
            )
            .collectList()
            .map {
                Status.status200Ok(it)
            }

    }

    /**
     * @comment 21.11.18 yj
     * @param : GET {{localhost}}/v3/session/{ids}/crash?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T23:59:59Z
     * response
     * data: [{
        "session_id": "62dcfdf2b188a3e07c180d675aa676637f22dccc",
        "session_time": "Apr 1, 2022, 10:50:00 PM",
        "device_lang": "en_US",
        "dwell_time": 51594,
        "view_count": 1,
        "event_count": 2,
        "unique_view_count": 1,
        "app_version": "1.0.1",
        "device_name": "goldfish_x86 sdk_gphone_x86",
        "device_wh": "1080x2280",
        "session_exp": 7,
        "crash_battery": 100,
        "crash_charge": 0,
        "crash_free_disk": 4961,
        "crash_disk": 5951,
        "crash_free_memory": 1036,
        "crash_memory": 1980,
        "crash_network": 20,
        "crash_process": 1,
        "device_zone": "GMT"
     * }]
     */
    fun crash(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids")

        val appIdList = req.splitQueryOrDefault("app_id_list")
        val fieldList = req.splitQueryOrDefault("field_list", "_id,aa,${P.di}")
        val sortField = req.getQueryOrDefault("sort_field", "_id")
        val sortValue = req.getQueryOrDefault("sort_value", 1)
        val fromDate = req.getQueryOrDefault("from_date")
        val toDate = req.getQueryOrDefault("to_date")
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 99)

        val validator = Validator()
            .new(limit, "limit").max(100)
            .new(fromDate, "from_date").date().required()
            .new(toDate, "to_date").date().required()

        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList =
                    if (appIdList.isNotEmpty()) appIdList.filter { ownedAppIdList.contains(it) } else ownedAppIdList

                val matchQuery = D()
                    .append(
                        P.st,
                        D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
                    )
                    .append(P.ai, D("\$in", QueryPeasant.convertToObjectIdList(appIdList)))

                if (ids.first() != "*") matchQuery.append(P.si, D("\$in", ids))

                val projectDoc = D().append("_id", 0)
                    .append(P.siK, "\$${P.si}")
                    .append(P.stzK, "\$${P.stz}")
                    .append(P.dlK, "\$session.${P.dl}")
                    .append(P.dtK, "\$event._id.${P.dt}")
                    .append(P.vcoK, "\$event._id.${P.vco}")
                    .append(P.ecoK, "\$event._id.${P.eco}")
                    .append(P.uvcoK, "\$event.${P.uvco}")
                    .append(P.avK, "\$${P.av}")
                    .append(P.dnK, "\$${P.dn}")
                    .append(
                        P.dwhK,
                        D(
                            "\$concat",
                            listOf(D("\$toString", "\$session.${P.dw}"), "x", D("\$toString", "\$session.${P.dh}"))
                        )
                    )
                    .append(P.seK, "\$session.${P.se}")
                    .append(P.dlK, "\$session.${P.dl}")
                    .append(P.cbK, "\$${P.cb}")
                    .append(P.ccK, "\$${P.cc}")
                    .append(P.cfdK, "\$${P.cfd}")
                    .append(P.ctdK, "\$${P.ctd}")
                    .append(P.cfmK, "\$${P.cfm}")
                    .append(P.ctmK, "\$${P.ctm}")
                    .append(P.cnK, "\$${P.cn}")
                    .append(P.cpK, "\$${P.cp}")
                    .append(P.dzK, "\$session.${P.dz}")

                val query = listOf(
                    D("\$match", matchQuery),
                    D(
                        "\$lookup", D()
                            .append("from", SessionService.COLLECTION_NAME)
                            .append("let", D().append(P.si, "\$${P.si}"))
                            .append(
                                "pipeline", listOf(
                                    D(
                                        "\$match", D(
                                            "\$expr", D(
                                                "\$and", listOf(
                                                    D("\$eq", listOf("\$_id", "\$\$${P.si}"))
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                            .append("as", "session")
                    ),
                    D("\$unwind", "\$session"),
                    D(
                        "\$lookup", D()
                            .append("from", EventService.COLLECTION_NAME)
                            .append("let", D().append(P.si, "\$${P.si}"))
                            .append(
                                "pipeline", listOf(
                                    D(
                                        "\$match", D(
                                            "\$expr", D(
                                                "\$and", listOf(
                                                    D("\$eq", listOf("\$_id.${P.si}", "\$\$${P.si}")),
                                                    D("\$in", listOf("\$${P.t}", listOf(ET.CRASH)))
                                                )
                                            )
                                        )
                                    ),
                                    D(
                                        "\$group", D()
                                            .append(
                                                "_id", D()
                                                    .append(P.si, "\$_id.${P.si}")
                                                    .append(P.vhi, "\$${P.vhi}")
                                                    .append(P.uts, "\$${P.uts}")
                                            )
                                            .append(P.eco, D("\$sum", 1))
                                            .append(P.dt, D("\$max", "\$_id.${P.ts}"))
                                    ),
                                    D("\$addFields", D(P.vco, 1)),
                                    D(
                                        "\$group", D()
                                            .append(
                                                "_id", D()
                                                    .append(P.si, "\$_id.${P.si}")
                                                    .append(P.vhi, "\$_id.${P.vhi}")
                                            )
                                            .append(P.eco, D("\$sum", "\$${P.eco}"))
                                            .append(P.vco, D("\$sum", "\$${P.vco}"))
                                            .append(P.dt, D("\$max", "\$${P.dt}"))
                                    ),
                                    D(
                                        "\$group", D()
                                            .append(
                                                "_id", D()
                                                    .append(P.si, "\$_id.${P.si}")
                                                    .append(P.eco, D("\$sum", "\$${P.eco}"))
                                                    .append(P.vco, D("\$sum", "\$${P.vco}"))
                                                    .append(P.dt, D("\$max", "\$${P.dt}"))
                                            )
                                            .append(P.uvco, D("\$sum", 1))
                                    )
                                )
                            )
                            .append("as", "event")
                    ),
                    D("\$unwind", "\$event"),
                    D("\$project", projectDoc),
                    D("\$skip", skip),
                    D("\$limit", limit)
                )

                Flux
                    .from(
                        MongodbUtil.getCollection(CrashList.COLLECTION_NAME_PT10M)
                            .aggregate(query)
                    )
                    .collectList()
                    .doOnError {
                        log.info(Util.toPretty(query))
                    }
                    .map {
                        Status.status200Ok(it)
                    }
                // end of crash
            }
    }

    /**
     * cruD 삭제
     * ids (mendatory) : ,(콤마) 구분자로 된 String value
     */
    fun delete(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids", "")

        val pub = MongodbUtil.getCollection(COLLECTION_NAME)
            .deleteMany(D("_id", D("\$in", ids)))

        return Mono.from(pub)
            .map {
                Status.status200Ok(it)
            }
    }

    /**
     * @author ALL
     *
     * @comment 21.11.18 yj
     * @sample GET {{localhost}}/v3/session/{ids}/count?from_date=2021-09-30T00:00:00Z&to_date=2021-12-31T23:59:59Z
     * @return data: [...]
     *{
    "app_id": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
    "session_time": {},
    "session_count": 15140
     *}
     */
    fun count(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val crashIdList = req.splitQueryOrDefault("crash_id_list", "")
        val viewId = req.getQueryOrDefault("view_id", "")
        val afterViewId = req.getQueryOrDefault("after_view_id", "")
        val fieldList = req.splitQueryOrDefault("field_list", "${P.scoK}")
        val by = req.getQueryOrDefault("by", "session")
        val duration = req.getQueryOrDefault("duration", "all")
        val isCrash = req.getQueryOrDefault("is_crash", "false").toBoolean()
        val osVersion = req.getQueryOrDefault("os_version", "")
        val deviceName = req.getQueryOrDefault("device_name", "")

        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val sortField = req.getQueryOrDefault("sort_field", "")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(by, "by").required(
                listOf(
                    "session",
                    "event",
                    "view",
                    "country",
                    "device_name",
                    "os",
                    "version",
                    "resolution",
                    "os_device_name",
                    "flow"
                )
            )
            .new(duration, "duration")
            .required(listOf("month", "week", "day", "all", "hour", "day_of_week", "real_time"))
            .new(limit, "limit").max(100)

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList =
                    if (appIdList.isNotEmpty()) appIdList.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

                val query: List<D>
                val coll: String
                val groupDuration = if (duration == "month") {
                    D("\$dateToString", D("format", "%Y-%m-01T00:00:00.000Z").append("date", "\$${P.stz}"))
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
                } else if (duration == "day") {
                    D("\$dateToString", D("format", "%Y-%m-%dT00:00:00.000Z").append("date", "\$${P.stz}"))
                } else if (duration == "hour") {
                    D("\$dateToString", D("format", "%H").append("date", "\$${P.stz}"))
                } else if (duration == "day_of_week") {
                    D("\$dayOfWeek", "\$${P.stz}")
                } else { // all
                    D()
                }

                /**
                 * @author LeeJaEun
                 */
                if ("session device_name os version resolution country os_device_name".contains(by)) {
                    val collSession: String

                    val groupSession = D(P.ai, "\$${P.ai}")
                    val groupCrash = D(P.ai, "\$${P.ai}")
                    val groupOuter = D(P.ai, "\$list._id.${P.ai}")

                    if (duration.isNotEmpty()) {
                        groupSession.append(P.stz, groupDuration)
                        groupCrash.append(P.stz, groupDuration)
                        groupOuter.append(P.stz, "\$list._id.${P.stz}")
                    }

                    if (by == "os") {
                        collSession =
                            if (duration == "hour") IndicatorSessionByOs.COLLECTION_NAME_PT10M
                            else IndicatorSessionByOs.COLLECTION_NAME_PT24H
                        groupSession.append(P.dov, "\$${P.dov}")
                        groupCrash.append(P.dov, "\$${P.dov}")
                        groupOuter.append(P.dov, "\$list._id.${P.dov}")
                    } else if (by == "device_name") {
                        collSession =
                            if (duration == "hour") IndicatorSessionByDevicename.COLLECTION_NAME_PT10M
                            else IndicatorSessionByDevicename.COLLECTION_NAME_PT24H
                        groupSession.append(P.dn, "\$${P.dn}")
                        groupCrash.append(P.dn, "\$${P.dn}")
                        groupOuter.append(P.dn, "\$list._id.${P.dn}")
                    } else if (by == "version") {
                        collSession =
                            if (duration == "hour") IndicatorAllByApp.COLLECTION_NAME_PT10M
                            else IndicatorAllByApp.COLLECTION_NAME_PT24H
                        groupSession.append(P.av, "\$${P.av}")
                        groupCrash.append(P.av, "\$${P.av}")
                        groupOuter.append(P.av, "\$list._id.${P.av}")
                    } else if (by == "resolution") {
                        collSession =
                            if (duration == "hour") IndicatorSessionByResolution.COLLECTION_NAME_PT10M
                            else IndicatorSessionByResolution.COLLECTION_NAME_PT24H
                        groupSession.append(P.dw, "\$${P.dw}")
                        groupSession.append(P.dh, "\$${P.dh}")
                        groupOuter.append(P.dw, "\$list._id.${P.dw}")
                        groupOuter.append(P.dh, "\$list._id.${P.dh}")
                    } else if (by == "country") {
                        collSession =
                            if (duration == "hour") IndicatorSessionByCountry.COLLECTION_NAME_PT10M
                            else IndicatorSessionByCountry.COLLECTION_NAME_PT24H
                        groupSession.append(P.dl, "\$${P.dl}")
                        groupCrash.append(P.dl, "\$${P.dl}")
                        groupOuter.append(P.dl, "\$list._id.${P.dl}")
                    } else if (by == "os_device_name") { // TODO test os_device_name
                        collSession =
                            if (duration == "hour") IndicatorSessionByOsDevicename.COLLECTION_NAME_PT10M
                            else IndicatorSessionByOsDevicename.COLLECTION_NAME_PT24H
                    } else {
                        collSession =
                            if (duration == "hour" || duration == "real_time") IndicatorAllByApp.COLLECTION_NAME_PT10M
                            else IndicatorAllByApp.COLLECTION_NAME_PT24H
                    }

                    val lookupMatchQuery = D()
                        .append(
                            P.stz,
                            D("\$gte", Date.from(Instant.parse(fromDate))).append(
                                "\$lte",
                                Date.from(Instant.parse(toDate))
                            )
                        )
                        .append(P.ai, D("\$in", appIdInObjIds))
                    if (versionList.isNotEmpty()) lookupMatchQuery.append(P.av, D("\$in", versionList))

                    // Todo : ccoK 즉 Crash 아닐 때 아래 쿼리 실행.
                    val sessionQuery = listOf(
                        D(
                            "\$lookup", D()
                                .append("from", collSession)
                                .append("let", D())
                                .append(
                                    "pipeline",
                                    listOf(
                                        D("\$match", lookupMatchQuery),
                                        D("\$group", D("_id", groupSession).append(P.sco, D("\$sum", "\$${P.sco}"))),
                                    )
                                )
                                .append("as", "list")
                        ),
                    )

                    query = if (fieldList.contains(P.ccoK)) {
                        val collCrash =
                            if (duration == "hour" || duration == "real_time") IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT10M
                            else IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H

                        val lookupMatchQuery = D()
                            .append(
                                P.stz,
                                D("\$gte", Date.from(Instant.parse(fromDate))).append(
                                    "\$lte",
                                    Date.from(Instant.parse(toDate))
                                )
                            )
                            .append(P.ai, D("\$in", appIdInObjIds))
                        if (versionList.isNotEmpty()) lookupMatchQuery.append(P.av, D("\$in", versionList))
                        // TODO sessionQuery에는 없는데 어케 처리할까?? > all
                        if (osVersion.isNotEmpty()) lookupMatchQuery.append(P.dov, D("\$eq", osVersion))
                        if (deviceName.isNotEmpty()) lookupMatchQuery.append(P.dn, D("\$eq", deviceName))
                        if (crashIdList.isNotEmpty()) lookupMatchQuery.append(
                            P.ci,
                            D("\$in", crashIdList.map { it.toLong() })
                        )

                        sessionQuery + listOf(
                            D(
                                "\$lookup", D()
                                    .append("from", collCrash)
                                    .append("let", D())
                                    .append(
                                        "pipeline", listOf(
                                            D("\$match", lookupMatchQuery),
                                            D(
                                                "\$group",
                                                D("_id", groupCrash).append(P.csco, D("\$sum", "\$${P.csco}"))
                                            ),
                                        )
                                    )
                                    .append("as", "crash")
                            ),
                            D("\$project", D("list", D("\$concatArrays", listOf("\$list", "\$crash")))),
                        )
                    } else {
                        sessionQuery
                    } + listOf(
                        D("\$unwind", "\$list"),
                        D("\$group", D("_id", groupOuter).let {
                            it.append(P.sco, D("\$sum", "\$list.${P.sco}"))
                            if (fieldList.contains(P.ccoK)) it.append(P.cco, D("\$sum", "\$list.${P.csco}"))
                            it
                        }
                        ),
                        D(
                            "\$sort", D(
                                if (sortField.isEmpty()) "_id.${P.stz}"
                                else if ("${P.stzK}${P.aiK}".contains(sortField)) "_id.${P.getAbbreviation(sortField)}"
                                else P.getAbbreviation(sortField), sortValue
                            )
                        ),
                        D("\$skip", skip),
                        D("\$limit", limit),
                        D(
                            "\$project", D()
                                .append(P.aiK, "\$_id.${P.ai}")
                                .append(P.stzK, "\$_id.${P.stz}")
                                .append(P.dnK, "\$_id.${P.dn}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.dwK, "\$_id.${P.dw}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.dhK, "\$_id.${P.dh}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.dovK, "\$_id.${P.dov}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.avK, "\$_id.${P.av}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.dlK, "\$_id.${P.dl}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.scoK, "\$${P.sco}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.ccoK, "\$${P.cco}")// group 에 없는 값은 자동으로 안보여짐
                                .append("_id", 0)
                        )
                    )

                    Flux
                        .from(MongodbUtil.getCollection("one").aggregate(query))
                        .collectList()
                        .doOnError {
                            log.info(Util.toPretty(query))
                        }
                        .map {
                            Status.status200Ok(it)
                        }
                    // end of all
                } else if (by == "flow") {
                    /**
                     * 동일흐름 반복 이동 세션 수 집계
                     * @author cjh
                     */

                    val matchQuery = D()
                        .append(
                            P.stz,
                            D("\$gte", Date.from(Instant.parse(fromDate))).append(
                                "\$lte",
                                Date.from(Instant.parse(toDate))
                            )
                        )
                        .append(P.ai, D("\$in", appIdInObjIds))
                    if (versionList.isNotEmpty()) matchQuery.append(P.av, D("\$in", versionList))
                    if (viewId.isNotEmpty()) matchQuery.append(P.vhi, D("\$eq", viewId.toLong()))
                    if (afterViewId.isNotEmpty()) matchQuery.append(P.avhi, D("\$eq", viewId.toLong()))

                    coll = IndicatorByEachFlow.COLLECTION_NAME_PT24H

                    query = listOf(
                        D("\$match", matchQuery),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.vi, "\$${P.vi}")
                                        .append(P.avhi, "\$${P.avhi}")
                                        .append(P.svi, "\$${P.svi}")
                                )
                                .append(P.se, D("\$sum", "\$${P.se}"))
                        ),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.vi, "\$${P.vi}")
                                        .append(P.avhi, "\$${P.avhi}")
                                )
                                .append(
                                    "${P.svi}_list", D(
                                        "\$push", D()
                                            .append(P.svi, "\$_id.${P.svi}")
                                            .append(P.se, "\$${P.se}")
                                    )
                                )
                                .append("total_${P.se}", D("\$sum", "\$${P.se}"))
                        ),

                        D("\$unwind", "\$${P.svi}_list"),

                        D(
                            "\$addFields", D()
                                .append(
                                    "sc_ratio",
                                    D(
                                        "\$multiply",
                                        listOf(D("\$divide", listOf("\$${P.svi}_list.${P.se}", "\$total_${P.se}")), 100)
                                    )
                                )
                        ),

                        D("\$sort", D("${P.svi}_list.${P.svi}", 1)),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.vi, "\$_id.${P.vi}")
                                        .append(P.avhi, "\$_id.${P.avhi}")
                                )
                                .append(
                                    "${P.svi}_list", D(
                                        "\$push", D()
                                            .append(P.svi, "\$${P.svi}_list.${P.svi}")
                                            .append(P.se, "\$${P.svi}_list.${P.se}")
                                            .append("sc_ratio", "\$sc_ratio")
                                    )
                                )
                        ),

                        D(
                            "\$project", D()
                                .append("_id", 0)
                                .append(P.viK, "\$_id.${P.vi}")
                                .append(P.avhiK, "\$_id.${P.avhi}")
                                .append("${P.svi}_list", "\$${P.svi}_list")
                        ),

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
                            log.info(Util.toJsonQuery(query, coll))
                        }
                        .map {
                            Status.status200Ok(it)
                        }
                    // end of flow
                } else if (by == "event") {
                    coll = ""
                    query = listOf(D())
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
// @deprecated, see '/v3/session/count/of-each-view'
//				else if (by == "view") {
//					coll = ""
//					query = listOf(D())
//					Flux
//						.from(MongodbUtil.getCollection(coll)
//							.aggregate(query)
//						)
//						.collectList()
//						.doOnError {
//							log.info(Util.toPretty(query))
//						}
//						.map {
//							Status.status200Ok(it)
//						}
                else {
                    coll = ""
                    query = listOf(D())

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


                // end of getAppIdList()
            }

        // end of count()
    }

    /**
     * @author cjh
     * @comment 21.11.18 yj
     * @sample GET {{localhost}}/v3/session/{ids}/rank?from_date=2021-09-30T00:00:00Z&to_date=2021-12-31T23:59:59Z
     * @param
     *  remove_session_start_end_view: ###SESSION_START###, ###SESSION_END###를 결과에서 지우는 파라미터
     *  regex_alias, regex_vi: 정규표현식으로 해당 스트링이 포함되는 결과를 필터링하는 파라미터
     * @return data: [
     * {
    "session_count": 15140,
    "view_hash_id": 74180013,
    "rank": 1,
    "view": {
    "_id": {
    "timestamp": 1636088476,
    "date": 1636088476000
    },
    "ai": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
    "av": "3.6.1",
    "vhi": 74180013,
    "alias": null,
    "favorite": false,
    "vi": "###SESSION_START###",
    "app_version": [
    3,
    6,
    1
    ]
    }
     *}, ...]
     */
    fun rank(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        // @deprecated
//		 val appIdList = req.splitParamOrDefault("ids", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val viewIdList = req.splitQueryOrDefault("view_id_list", "")
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)
        val by = req.getQueryOrDefault("by", "view")
        val type = req.getQueryOrDefault("type", "all")
        val isCrash = req.getQueryOrDefault("is_crash", "false").toBoolean()
        val sessionStartAndEndFlag = req.getQueryOrDefault("remove_session_start_end_view", "false").toBoolean()
        val searchRegexByAlias = req.getQueryOrDefault("regex_alias", "")
        val searchRegexByVi = req.getQueryOrDefault("regex_vi", "")

        val vali = Validator()
//			.new(appIdList, "ids").required()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(by, "by").required(listOf("view"))
            .new(type, "type").required(listOf("all", "termination"))

        if (type == "termination") {
            vali.new(isCrash, "is_crash").must(false)
        }

        val regexDocByVi = if (searchRegexByVi.isNotEmpty()) D(mapOf("\$regexMatch" to mapOf("input" to "\$view.vi", "regex" to searchRegexByVi))) else D()
        val regexDocByAlias = if (searchRegexByAlias.isNotEmpty()) D(mapOf("\$regexMatch" to mapOf("input" to "\$view.alias", "regex" to searchRegexByAlias))) else D()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()
        val filterDoc =
            if (viewIdList.isNotEmpty()) D("\$in", listOf("\$${P.vhiK}", viewIdList.map { it.toLong() })) else D()
        // lookup 하위 pipeline $match $expr 사용 시에 $nin, $regex 사용 불가

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList =
                    if (appIdList.isNotEmpty()) appIdList.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
//				val appIdList = req.splitParamOrDefault("ids", "").let { ailParam ->
//					if (ailParam.isNotEmpty() && ailParam.first() != "*") ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
//				}

                // 뷰당 세션 랭크 지표, 크래시 세션 랭크 지표, 앱 종료 세션 랭크 지표
                val (coll, query) = if (by == "view") {
                    if (type == "all") {
                        if (isCrash) {
                            val matchQuery = this._getMatchPipe(
                                fromDate,
                                toDate,
                                appIdInObjIds,
                                versionList,
                                sessionStartAndEndFlag
                            )

                            IndicatorAllByView.COLLECTION_NAME_PT24H to listOf(
                                D("\$match", matchQuery),

                                D(
                                    "\$group", D()
                                        .append(
                                            "_id", D()
                                                .append(P.vhi, "\$${P.vhi}")
                                        )
                                        .append(P.cco, D("\$sum", "\$${P.cco}"))
                                ),

                                D(
                                    "\$group", D()
                                        .append(
                                            "_id", D()
                                                .append(P.cco, "\$${P.cco}")
                                        )
                                        .append(
                                            "arr", D()
                                                .append("\$push", D(P.vhi, "\$_id.${P.vhi}"))
                                        )
                                ),

                                D("\$sort", D("_id.${P.cco}", -1)),

                                D(
                                    "\$group", D()
                                        .append("_id", 1)
                                        .append(
                                            "arr", D()
                                                .append("\$push", D(P.cco, "\$_id.${P.cco}").append("arr", "\$arr"))
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
                                        .append(P.ccoK, "\$arr.${P.cco}")
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
                                                    D("${P.avK}.0", -1).append("${P.avK}.1", -1)
                                                        .append("${P.avK}.2", -1).append("${P.avK}.3", -1)
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
                                                filterDoc,
                                                regexDocByVi,
                                                regexDocByAlias
                                            )
                                        )
                                    )
                                ),

                                D("\$skip", skip),
                                D("\$limit", limit)
                            )
                        } else {
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
                                        .append(P.sco, D("\$sum", "\$${P.sco}"))
                                ),

                                D(
                                    "\$group", D()
                                        .append(
                                            "_id", D()
                                                .append(P.sco, "\$${P.sco}")
                                        )
                                        .append(
                                            "arr", D()
                                                .append("\$push", D(P.vhi, "\$_id.${P.vhi}"))
                                        )
                                ),

                                D("\$sort", D("_id.${P.sco}", -1)),

                                D(
                                    "\$group", D()
                                        .append("_id", 1)
                                        .append(
                                            "arr", D()
                                                .append("\$push", D(P.sco, "\$_id.${P.sco}").append("arr", "\$arr"))
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
                                        .append(P.scoK, "\$arr.${P.sco}")
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
                                                    D("${P.avK}.0", -1).append("${P.avK}.1", -1)
                                                        .append("${P.avK}.2", -1).append("${P.avK}.3", -1)
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
                                                filterDoc,
                                                regexDocByVi,
                                                regexDocByAlias
                                            )
                                        )
                                    )
                                ),

                                D("\$skip", skip),
                                D("\$limit", limit)
                            )
                        }
                    } else if (type == "termination") {
                        if (isCrash) {
                            "" to listOf(D())
                        } else {

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
                                        .append(P.tco, D("\$sum", "\$${P.tco}"))
                                ),

                                D(
                                    "\$group", D()
                                        .append(
                                            "_id", D()
                                                .append(P.tco, "\$${P.tco}")
                                        )
                                        .append(
                                            "arr", D()
                                                .append("\$push", D(P.vhi, "\$_id.${P.vhi}"))
                                        )
                                ),

                                D("\$sort", D("_id.${P.tco}", -1)),

                                D(
                                    "\$group", D()
                                        .append("_id", 1)
                                        .append(
                                            "arr", D()
                                                .append("\$push", D(P.tco, "\$_id.${P.tco}").append("arr", "\$arr"))
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
                                        .append(P.tcoK, "\$arr.${P.tco}")
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
                                                    D("${P.avK}.0", -1).append("${P.avK}.1", -1)
                                                        .append("${P.avK}.2", -1).append("${P.avK}.3", -1)
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
                                                filterDoc,
                                                regexDocByVi,
                                                regexDocByAlias
                                            )
                                        )
                                    )
                                ),

                                D("\$skip", skip),
                                D("\$limit", limit)
                            )
                        }
                    } else {
                        "" to listOf(D())
                    }
                } else {
                    "" to listOf(D())
                }

                Flux
                    .from(
                        MongodbUtil.getCollection(coll)
                            .aggregate(query)
                    )
                    .collectList()
                    .map {
                        log.info(it.toString())
                        Status.status200Ok(it)
                    }
            }
        // end of rank()
    }

    /**
     * @comment 21.11.18 yj
     * @sample GET {{localhost}}/v3/session/{ids}/open?app_key=e6c101f020e1018b5ba17cdbe32ade2d679b44bc
     * reponse: data: [...]
     * {
    "app_key": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
    "authed": true,
    "kill_switch": "please, don't die :(",
    "config": "everything"
     * }
     */
    fun open(req: SafeRequest): Mono<Map<String, Any>> {
        val appKeyList = req.splitQueryOrDefault(P.akK, "")

        val vali = Validator()
        appKeyList.forEach {
            vali.new(it, P.akK).required()
        }
        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        return AppService.getAppIdMap(appKeyList)
            .map { appIdMap ->
                Status.status200Ok(
                    appKeyList.map {
                        mapOf(
                            P.akK to it,
                            "authed" to appIdMap.containsKey(it),
                            "kill_switch" to "please, don't die :(",
                            "config" to "everything"
                        )
                    }
                )
            }
    }


    fun getCountOfEachScreen(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")

        val vali = Validator()
            .new(appIdList, "app_id_list").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val sessCountServ = SessionServiceSessCount()
        val mongoResStream = sessCountServ.getMongoResStreamSessCountOfEachScreen(
            appIdStream, appIdList, versionList
        )
        val respStream = mongoResStream.map { result ->
            log.debug(QueryPeasant.listDocToStr(result))
            Status.status200Ok(result)
        }
        return respStream
    }


    fun getCountOfDeviceId(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val duration = req.getQueryOrDefault("duration", "day")

        val vali = Validator()
            .new(fromDate, "from_date").required()
            .new(toDate, "to_date").required()
            .new(duration, "duration").required(listOf("hour", "day", "month"))
        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val sessCountServ = SessionServiceSessCount()
        val mongoResStream = sessCountServ.getMongoResStreamSessCountOfDeviceId(
            appIdStream, appIdList, versionList,
            listOf(Date.from(Instant.parse(fromDate)), Date.from(Instant.parse(toDate))),
            duration
        )
        val respStream = mongoResStream.map { result ->
            log.debug(QueryPeasant.listDocToStr(result))
            Status.status200Ok(result)
        }
        return respStream

    }

    fun getTheLast(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val deviceId = req.getQueryOrDefault("device_id", "")

        val vali = Validator()
            .new(appIdList, "app_id_list").required()
            .new(deviceId, "device_id").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val sessCountServ = SessionServiceSessionInfo()
        val mongoResStream = sessCountServ.getMongoResStreamLastSession(
            appIdStream, appIdList, versionList,
            deviceId
        )
        val respStream = mongoResStream.map { result ->
            log.debug(QueryPeasant.listDocToStr(result))
            Status.status200Ok(result)
        }
        return respStream
    }

    fun getTheFirst(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val deviceId = req.getQueryOrDefault("device_id", "")

        val vali = Validator()
            .new(appIdList, "app_id_list").required()
            .new(deviceId, "device_id").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val sessCountServ = SessionServiceSessionInfo()
        val mongoResStream = sessCountServ.getMongoResStreamFirstSession(
            appIdStream, appIdList, versionList,
            deviceId
        )
        val respStream = mongoResStream.map { result ->
            log.debug(QueryPeasant.listDocToStr(result))
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
