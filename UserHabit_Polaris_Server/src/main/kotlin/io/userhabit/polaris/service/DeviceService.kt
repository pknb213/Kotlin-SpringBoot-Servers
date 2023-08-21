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
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.*
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P

object DeviceService {
    private val log = Loggers.getLogger(DeviceService::class.java)
    // @deprecated
    // lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>

    /**
     * 대시보드
     * 앱 활성도 DAU, WAU, MAU part.
     * @author LeeJaeun
     * @comment 21.11.18 yj
     * @sample [GET {{localhost}}/v3/device/{ids}/count?app_id_list=e6c101f020e1018b5ba17cdbe32ade2d679b44bc&from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z]
     * @return data = [...]
     * {
    "app_id": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
    "session_time": "2021-11-01T00:00:00Z",
    "all_device_count": 426,
    "re_device_count": 426,
    "new_device_count": 0
     * }
     */
    fun count(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val crashIdList = req.splitQueryOrDefault("crash_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fieldList = req.splitQueryOrDefault("field_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val sortField = req.getQueryOrDefault("sort_field", "")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)

        val duration = req.getQueryOrDefault("duration", "month")
        val type = req.getQueryOrDefault("type", "")
        val by = req.getQueryOrDefault("by", "device")
        val isCrash = req.getQueryOrDefault("is_crash", "false").toBoolean()
        val isUnique = req.getQueryOrDefault("is_unique", "false").toBoolean()
        val osVersion = req.splitQueryOrDefault("os_version", "")
        val deviceName = req.splitQueryOrDefault("device_name", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(duration, "duration").required(listOf("all", "hour", "day", "week", "month", "real_time"))
            .new(limit, "limit").max(100)
            .new(by, "by").required(listOf("device", "os_device_name"))

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

//				val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()
//				val deviceNameDoc = if (deviceName.isNotEmpty()) D("\$in", listOf("\$${P.dn}", deviceName)) else D()
//				val osVersionDoc = if (osVersion.isNotEmpty()) D("\$in", listOf("\$${P.dov}", osVersion)) else D()
//				val crashIdDoc = if (crashIdList.isNotEmpty()) D("\$in", listOf("\$${P.ci}", crashIdList.map{ it. toLong()})) else D()

                val query: List<D>
                val coll: String

                val groupDoc = if (duration == "month") {
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
                    D("\$dateToString", D("format", "%Y-%m-%dT%H:00:00.000Z").append("date", "\$${P.stz}"))
                } else { // all
                    D()
                }

                /**
                 * @author who?? LeeJaEun?? maybe..
                 */
                if (by == "device") {
                    coll = "one"
                    val group = D("_id", D(P.ai, "\$${P.ai}").append(P.stz, groupDoc))
                    val groupCrash = D()

                    fieldList.forEach { key ->
                        val abbr = P.getAbbreviation(key)
                        group.append(abbr, D("\$sum", "\$${abbr}"))
                        groupCrash.append(abbr, D("\$sum", "\$list.${abbr}"))
                    }
                    group.remove(P.cco)
                    groupCrash.remove(P.cco)

                    val (collDevice, collCrash) = if (duration == "hour" || duration == "real_time") {
                        IndicatorDeviceByApp.COLLECTION_NAME_PT10M to IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT10M
                    } else {
                        IndicatorDeviceByApp.COLLECTION_NAME_PT24H to IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H
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

                    if (versionList.isNotEmpty()) {
                        lookupMatchQuery.append(P.av, D("\$in", versionList))
                    }

                    val deviceQuery = listOf(
                        D(
                            "\$lookup", D()
                                .append("from", collDevice)
                                .append("let", D())
                                .append(
                                    "pipeline", listOf(
                                        D("\$match", lookupMatchQuery),
                                        D(
                                            "\$group",
                                            group + D(P.adco, D("\$sum", "\$${P.adco}")).append(
                                                P.ndco,
                                                D("\$sum", "\$${P.ndco}")
                                            )
                                                .append(P.rdco, D("\$sum", "\$${P.rdco}"))// adco는 기본으로 나와야 함
                                        ),
                                    )
                                )
                                .append("as", "list")
                        ),
                    )

                    query = if (fieldList.contains(P.ccoK)) {
                        groupCrash.append(P.cdco, D("\$sum", "\$list.${P.cdco}"))

                        val lookupMatchQuery = D()
                            .append(
                                P.stz,
                                D("\$gte", Date.from(Instant.parse(fromDate))).append(
                                    "\$lte",
                                    Date.from(Instant.parse(toDate))
                                )
                            )
                            .append(P.ai, D("\$in", appIdInObjIds))

                        if (versionList.isNotEmpty()) {
                            lookupMatchQuery.append(P.av, D("\$in", versionList))
                        }

                        // deviceQuery에는 없는데 어케 할까?? > all
                        if (crashIdList.isNotEmpty()) {
                            lookupMatchQuery.append(P.ci, D("\$in", crashIdList.map { it.toLong() }))
                        }

                        deviceQuery + listOf(
                            D(
                                "\$lookup", D()
                                    .append("from", collCrash)
                                    .append("let", D())
                                    .append(
                                        "pipeline", listOf(
                                            D("\$match", lookupMatchQuery),
                                            D(
                                                "\$group", D("_id", D(P.ai, "\$${P.ai}").append(P.stz, groupDoc))
                                                    .append(P.cdco, D("\$sum", "\$${P.cdco}")) // cdco는 기본으로 나와야 함
                                            ),
                                        )
                                    )
                                    .append("as", "crash")
                            ),
                            D("\$project", D("list", D("\$concatArrays", listOf("\$list", "\$crash")))),
                        )
                    } else {
                        deviceQuery
                    } + listOf(
                        D("\$unwind", "\$list"),
                        D(
                            "\$group",
                            D(
                                "_id", D(P.ai, "\$list._id.${P.ai}")
                                    .append(P.stz, "\$list._id.${P.stz}")
                            )
                                .append(P.adco, D("\$sum", "\$list.${P.adco}"))
                                .append(P.rdco, D("\$sum", "\$list.${P.rdco}"))
                                .append(P.ndco, D("\$sum", "\$list.${P.ndco}"))
                                    + groupCrash // adco 기본으로 나와야 함
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
                            "\$project", D("_id", 0)
                                .append(P.aiK, "\$_id.${P.ai}")
                                .append(P.stzK, "\$_id.${P.stz}")
                                .append(P.adcoK, "\$${P.adco}")
                                .append(P.rdcoK, "\$${P.rdco}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.ndcoK, "\$${P.ndco}")// group 에 없는 값은 자동으로 안보여짐
                                .append(P.ccoK, "\$${P.cdco}") // group 에 없는 값은 자동으로 안보여짐
                        )
                    )

//					println(Util.toPretty(query))
                } else if (by == "os_device_name") {
                    val group: D
                    val projection: D
                    val sort: D

                    if (isCrash) {
                        if (type == "hour") {
                            coll = IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT10M

                            projection = D().append("_id", 0)
                                .append("hour", "\$_id.hour")
                                .append(P.cdcoK, "\$${P.cdco}")

                            group = D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(
                                                "hour",
                                                D("\$dateToString", D("format", "%H").append("date", "\$${P.stz}"))
                                            )
                                    )
                                    .append(P.cdco, D("\$sum", 1))
                            )
                            sort = D("\$sort", D("hour", 1))

                        } else {
                            coll = IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H

                            projection = D().append("_id", 0)
                                .append(P.stzK, "\$_id.${P.stz}")
                                .append(P.cdcoK, "\$${P.cdco}")

                            group = D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.stz, groupDoc)
                                    )
                                    .append(P.cdco, D("\$sum", "\$${P.cdco}"))
                            )
                            sort = D("\$sort", D(P.stzK, 1))
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

                        if (versionList.isNotEmpty()) {
                            matchQuery.append(P.av, D("\$in", versionList))
                        }

                        if (osVersion.isNotEmpty()) {
                            matchQuery.append(P.dov, D("\$in", osVersion))
                        }

                        if (deviceName.isNotEmpty()) {
                            matchQuery.append(P.dn, D("\$in", deviceName))
                        }

                        if (crashIdList.isNotEmpty()) {
                            matchQuery.append(P.ci, D("\$in", crashIdList.map { it.toLong() }))
                        }

                        val common = D("\$match", matchQuery)

                        query =
                            listOf(common, group, D("\$project", projection), D("\$skip", skip), D("\$limit", limit), sort)
                    } else {
                        //no crash, no type, JUST COUNTING filtered OS VERSION AND DEVICE NAME
                        coll = IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT24H
                        val projectDoc = D().append("_id", 0)
                            .append(P.stzK, "\$_id.${P.stz}")
                            .append(P.dcoK, "\$${P.dco}")

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

                        if (osVersion.isNotEmpty()) {
                            matchQuery.append(P.dov, D("\$in", osVersion))
                        }

                        if (deviceName.isNotEmpty()) {
                            matchQuery.append(P.dn, D("\$in", deviceName))
                        }

                        query = listOf(
                            D("\$match", matchQuery),
                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.stz, groupDoc)
                                    )
                                    .append(P.dco, D("\$sum", "\$${P.dco}"))
                            ),
                            D("\$project", projectDoc),
                            D("\$skip", skip),
                            D("\$limit", limit),
                            D("\$sort", D(P.stzK, 1))
                        )
                    }
                } else {
                    coll = ""
                    query = listOf(D())
                }

                // println(Util.toPretty(query))
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
     * @comment 21.11.18 yj
     * @sample [GET {{localhost}}/v3/device/{ids}/rank?app_id_list=e6c101f020e1018b5ba17cdbe32ade2d679b44bc&from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z]
     * @param remove_session_start_end_view: ###SESSION_START###, ###SESSION_END###를 결과에서 지우는 파라미터
     * @return data=[...]
     * {
     *	"value": 11.333333333333334,
     *	"view_hash_id": 306589677,
     *	"rank": 1,
     *	"view": {
     *		"_id": {
     *			"timestamp": 1636610884,
     *			"date": 1636610884000
     *		},
     *		"ai": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
     *		"av": "3.6.1",
     *		"vhi": 306589677,
     *		"alias": null,
     *		"favorite": false,
     *		"vi": "com.hdsec.android.mainlib.SmartActivitySM020000",
     *		"app_version": [
     *			3,
     *			6,
     *			1
     *		]
     *	}
     * },
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
        val type = req.getQueryOrDefault("type", "average")
        val sessionStartAndEndFlag = req.getQueryOrDefault("remove_session_start_end_view", "false").toBoolean()

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(by, "by").required(listOf("view"))
            .new(type, "type").required(listOf("average"))

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())


        val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()
        val filterDoc =
            if (viewIdList.isNotEmpty()) D("\$in", listOf("\$${P.vhiK}", viewIdList.map { it.toLong() })) else D()

        var coll: String
        var query: List<D>

        val dayRange = Period.between(
            LocalDate.ofInstant(Instant.parse(fromDate), ZoneId.systemDefault()),
            LocalDate.ofInstant(Instant.parse(toDate), ZoneId.systemDefault())
        ).days.let { if (it != 0) it else 1 }

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }

                if (by == "view") {
                    if (type == "average") {
                        coll = IndicatorDeviceByView.COLLECTION_NAME_PT24H
                        val matchQuery =
                            this._getMatchPipe(
                                fromDate,
                                toDate,
                                QueryPeasant.convertToObjectIdList(appIdList),
                                versionList,
                                sessionStartAndEndFlag
                            )

                        query = listOf(
                            D("\$match", matchQuery),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.vhi, "\$${P.vhi}")
                                    )
                                    .append("total", D("\$sum", "\$${P.dco}"))
                            ),

                            D(
                                "\$addFields", D()
                                    .append(P.dco, D("\$divide", listOf("\$total", dayRange)))
                            ),

                            D(
                                "\$group", D()
                                    .append(
                                        "_id", D()
                                            .append(P.dco, "\$${P.dco}")
                                    )
                                    .append(
                                        "arr", D()
                                            .append("\$push", D(P.vhi, "\$_id.${P.vhi}"))
                                    )
                            ),

                            D("\$sort", D("_id.${P.dco}", -1)),

                            D(
                                "\$group", D()
                                    .append("_id", 1)
                                    .append(
                                        "arr", D()
                                            .append("\$push", D(P.dco, "\$_id.${P.dco}").append("arr", "\$arr"))
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
                                    .append("value", "\$arr.${P.dco}")
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

    fun _getMatchPipe(
        fromDate: String,
        toDate: String,
        appIdInObjIds: List<ObjectId>,
        versionList: List<String>,
        sessionStartAndEndFlag: Boolean
    ): D {
        val matchQuery = D()
            .append(
                P.stz, D("\$gte", Date.from(Instant.parse(fromDate)))
                    .append("\$lte", Date.from(Instant.parse(toDate)))
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
