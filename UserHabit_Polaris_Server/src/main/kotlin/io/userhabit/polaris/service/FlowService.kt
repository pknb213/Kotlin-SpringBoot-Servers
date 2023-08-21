package io.userhabit.polaris.service

import io.userhabit.batch.indicators.*
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.service.flow.FlowServiceGetGraph
import io.userhabit.polaris.service.flow.FlowServiceGetTarget
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P


/**
 * 흐름 지표
 * @author cjh
 */
object FlowService {

    private val log = Loggers.getLogger(this.javaClass)
    // @deprecated
    // lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>

    /**
     * @author cjh
     * @comment 21.11.18 yj
     * @sample All Flow: GET {{localhost}}/v3/flow/{ids}?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z
     * @sample2 Target Flow: GET {{localhost}}/v3/flow/{ids}?type=target&from_view_id=74180013&to_view_id=-1671032428&from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z
     * @return data=[
     * {
     *  "flow": [
     *    {
     *      "view_hash_id": "1037659796",
     *      "alias": null,
     *      "view_id": "com.hdsec.android.mainlib.SmartActivityGM601004",
     *      "flow": [
     *        {
     *          "view_hash_id": "-1671032428",
     *          "alias": null,
     *          "view_id": "###SESSION_END###",
     *          "flow_count": 29
     *        }
     *      ],
     *      "flow_count": 29
     *    },
     *    {
     *      "view_hash_id": "305666342",
     *      "alias": null,
     *      "view_id": "com.hdsec.android.mainlib.SmartActivitySM010060",
     *      "flow": [
     *        {
     *          "view_hash_id": "305666161",
     *          "alias": null,
     *          "view_id": "com.hdsec.android.mainlib.SmartActivitySM010005",
     *          "flow_count": 29
     *        },
     *        {
     *          "view_hash_id": "306589677",
     *          "alias": null,
     *          "view_id": "com.hdsec.android.mainlib.SmartActivitySM020000",
     *          "flow_count": 56
     *        }
     *      ],
     *      "flow_count": 85
     *    }, ...
     *  ],
     *  "view_hash_id": "74180013",
     *  "alias": null,
     *  "view_id": "###SESSION_START###",
     *  "flow_count": 15130
     * }]
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val flowIdList = req.getParamOrDefault("ids", "")

        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val versionList = req.splitQueryOrDefault("version_list", "")

        val fromViewId = req.getQueryOrDefault("from_view_id", "")
        val toViewId = req.getQueryOrDefault("to_view_id", "")

        val viewId = req.getQueryOrDefault("view_id", "")
        val type = req.getQueryOrDefault("type", "all")

        val sortField = req.getQueryOrDefault("sort_key", "ratio")
        val sortValue = req.getQueryOrDefault("sort_value", -1)

        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)

        val fieldList = req.splitQueryOrDefault("field_list", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(limit, "limit").max(100)
            .new(type, "type").required(listOf("all", "after", "before", "target"))

        if (type == "before") {
            vali
                .new(viewId, "view_id").required()
                .new(sortField, "sort_key").required(listOf("ratio"))
        }
        if (type == "after") {
            vali
                .new(viewId, "view_id").required()
                .new(sortField, "sort_key").required(listOf("ratio"))
        }
        if (type == "target") {
            vali
                .new(fromViewId, "from_view_id").required()
                .new(toViewId, "to_view_id").required()
        }

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())
        if (type == "target") {
            val flowServ = FlowServiceGetTarget()
            val mongoResStream = flowServ.getMongoResStreamTargetFlow(
                appIdStream,
                appIdList, versionList, fromDate, toDate, fromViewId, toViewId,
                skip, limit
            )
            val respStream = mongoResStream.map { result ->
                println(result)
                Status.status200Ok(result)
            }
            return respStream

        }

        val startIndex = if (flowIdList.isNotEmpty()) flowIdList.split(",").size else 1
        val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()
        val regexFlow = D("\$regex", "^,${flowIdList}.*")

        return appIdStream.flatMap { ownedAppIdList ->
            val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
            }
            val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

            val (coll, query) = if (type == "all") {
                /**
                 * 전체사용흐름
                 * 이전 화면의 흐름의 스크린 키를 전달받아 해당 지점 포함 3단계의 모든 흐름과 집계 결과를 반환
                 */

                val matchQuery = D()
                    .append(
                        P.stz,
                        D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
                    )
                    .append(P.ai, D("\$in", appIdInObjIds))
                    .append("flow", regexFlow)

                if (versionList.isNotEmpty()) {
                    matchQuery.append(P.av, D("\$in", versionList))
                }

                IndicatorByFlow.COLLECTION_NAME_PT24H to listOf(
                    D("\$match", matchQuery),

                    D(
                        "\$group", D()
                            .append(
                                "_id", D()
//                .append("flow_detail", D("\$slice", listOf(D("\$split", listOf("\$flow", ",")), startIndex, 3))))
                                    .append(
                                        "flow_detail", D(
                                            "\$slice", listOf(
                                                D("\$split", listOf("\$flow", ",")), startIndex, mapOf(
                                                    "\$cond" to listOf(
                                                        mapOf(
                                                            "\$gte" to listOf(
                                                                mapOf(
                                                                    "\$size" to mapOf(
                                                                        "\$split" to listOf(
                                                                            "\$flow",
                                                                            ","
                                                                        )
                                                                    )
                                                                ), 4
                                                            )
                                                        ), // 최소 4개 이상 있어야 첫 번째 View로 부터 뒤에 3개까지 가져올 수 있기 때문
                                                        3,
                                                        1
                                                    )
                                                )
                                            )
                                        )
                                    )
                            )
//                .append("flow_detail", D("\$split", listOf("\$flow", ","))))
                            .append(P.fco, D("\$sum", "\$${P.fco}"))
                            .append(P.av, D("\$max", "\$${P.av}"))
                    ),
//             Todo : 여기서 _id : {} 이놈들 제거 해야함
                    D(
                        "\$lookup", D()
                            .append("from", ViewList.COLLECTION_NAME_PT24H)
                            .append(
                                "let", D(
                                    P.vhi, D(
                                        "\$convert", D("input", D("\$arrayElemAt", listOf("\$_id.flow_detail", 2)))
                                            .append("to", "int").append("onError", "error")
                                    )
                                )
                                    .append(P.av, "\$${P.av}")
                            )
                            .append(
                                "pipeline", listOf(
                                    D(
                                        "\$match", D(
                                            "\$expr", D(
                                                "\$and", listOf(
                                                    D("\$in", listOf("\$${P.ai}", appIdInObjIds)),
                                                    D("\$eq", listOf("\$${P.av}", "\$\$${P.av}")),
                                                    D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                                )
                                            )
                                        )
                                    ),
                                )
                            )
                            .append("as", "view_list")
                    ),

                    D(
                        "\$group", D()
                            .append(
                                "_id", D()
                                    .append("flow_detail", D("\$slice", listOf("\$_id.flow_detail", 2)))
                            )
                            .append(
                                "flow", D(
                                    "\$push", D()
                                        .append(P.vhiK, D("\$arrayElemAt", listOf("\$_id.flow_detail", 2)))
                                        .append("alias", D("\$arrayElemAt", listOf("\$view_list.alias", 0)))
                                        .append(P.viK, D("\$arrayElemAt", listOf("\$view_list.${P.vi}", 0)))
                                        .append(P.fcoK, "\$${P.fco}")
                                )
                            )
                            .append(P.fco, D("\$sum", "\$${P.fco}"))
                            .append(P.av, D("\$max", "\$${P.av}"))
                    ),

                    D(
                        "\$lookup", D()
                            .append("from", ViewList.COLLECTION_NAME_PT24H)
                            .append(
                                "let", D(
                                    P.vhi, D(
                                        "\$convert", D("input", D("\$arrayElemAt", listOf("\$_id.flow_detail", 1)))
                                            .append("to", "int").append("onError", "error")
                                    )
                                )
                                    .append(P.av, "\$${P.av}")
                            )
                            .append(
                                "pipeline", listOf(
                                    D(
                                        "\$match", D(
                                            "\$expr", D(
                                                "\$and", listOf(
                                                    D("\$in", listOf("\$${P.ai}", appIdInObjIds)),
                                                    D("\$eq", listOf("\$${P.av}", "\$\$${P.av}")),
                                                    D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                                )
                                            )
                                        )
                                    ),
                                )
                            )
                            .append("as", "view_list")
                    ),

                    D(
                        "\$group", D()
                            .append(
                                "_id", D()
                                    .append("flow_detail", D("\$slice", listOf("\$_id.flow_detail", 1)))
                            )
                            .append(
                                "flow", D(
                                    "\$push", D()
                                        .append(P.vhiK, D("\$arrayElemAt", listOf("\$_id.flow_detail", 1)))
                                        .append("alias", D("\$arrayElemAt", listOf("\$view_list.alias", 0)))
                                        .append(P.viK, D("\$arrayElemAt", listOf("\$view_list.${P.vi}", 0)))
                                        .append("flow", "\$flow")
                                        .append(P.fcoK, "\$${P.fco}")
                                )
                            )
                            .append(P.fco, D("\$sum", "\$${P.fco}"))
                            .append(P.av, D("\$max", "\$${P.av}"))
                    ),

                    D(
                        "\$lookup", D()
                            .append("from", ViewList.COLLECTION_NAME_PT24H)
                            .append(
                                "let", D(
                                    P.vhi, D(
                                        "\$convert", D("input", D("\$arrayElemAt", listOf("\$_id.flow_detail", 0)))
                                            .append("to", "int").append("onError", "error")
                                    )
                                )
                                    .append(P.av, "\$${P.av}")
                            )
                            .append(
                                "pipeline", listOf(
                                    D(
                                        "\$match", D(
                                            "\$expr", D(
                                                "\$and", listOf(
                                                    D("\$in", listOf("\$${P.ai}", appIdInObjIds)),
                                                    D("\$eq", listOf("\$${P.av}", "\$\$${P.av}")),
                                                    D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                                )
                                            )
                                        )
                                    ),
                                )
                            )
                            .append("as", "view_list")
                    ),

                    D(
                        "\$project", D()
                            .append("_id", 0)
                            .append(P.vhiK, D("\$arrayElemAt", listOf("\$_id.flow_detail", 0)))
                            .append("alias", D("\$arrayElemAt", listOf("\$view_list.alias", 0)))
                            .append(P.viK, D("\$arrayElemAt", listOf("\$view_list.${P.vi}", 0)))
                            .append(P.fcoK, "\$${P.fco}")
                            .append("flow", 1)
                    ),

                    D("\$skip", skip),
                    D("\$limit", limit)
                )
                // end of all
            } else if (type == "after") {
                /**
                 * 화면 분석
                 * 다음 화면 집계
                 */

                val matchQuery = D()
                    .append(
                        P.stz,
                        D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
                    )
                    .append(P.ai, D("\$in", appIdInObjIds))
                    .append(P.vhi, D("\$eq", viewId.toLong()))

                if (versionList.isNotEmpty()) {
                    matchQuery.append(P.av, D("\$in", versionList))
                }

                IndicatorByEachFlow.COLLECTION_NAME_PT24H to listOf(
                    D("\$match", matchQuery),

                    D(
                        "\$group", D()
                            .append(
                                "_id", D()
                                    .append(P.avhi, "\$${P.avhi}")
                            )
                            .append(P.mco, D("\$sum", "\$${P.mco}"))
                    ),

                    D(
                        "\$group", D()
                            .append("_id", 0)
                            .append(
                                "data", D()
                                    .append(
                                        "\$push", D()
                                            .append(P.avhi, "\$_id.${P.avhi}")
                                            .append(P.mco, "\$${P.mco}")
                                    )
                            )
                            .append("total_${P.mco}", D("\$sum", "\$${P.mco}"))
                    ),

                    D("\$unwind", "\$data"),

                    D(
                        "\$project", D()
                            .append("_id", 0)
                            .append(P.avhi, "\$data.${P.avhi}")
                            .append(P.mcoK, "\$data.${P.mco}")
                            .append(
                                "ratio",
                                D(
                                    "\$multiply",
                                    listOf(D("\$divide", listOf("\$data.${P.mco}", "\$total_${P.mco}")), 100)
                                )
                            )
                    ),

                    D(
                        "\$lookup", D()
                            .append("from", ViewList.COLLECTION_NAME_PT24H)
                            .append("let", D(P.vhi, "\$${P.avhi}"))
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
                        "\$project", D()
                            .append(P.aiK, "\$${P.ai}")
                            .append(P.avhiK, "\$${P.avhi}")
                            .append(P.mcoK, "\$${P.mcoK}")
                            .append("ratio", "\$ratio")
                            .append("view", "\$view")
                    ),

                    D("\$sort", D(sortField, sortValue)),

                    D("\$skip", skip),
                    D("\$limit", limit)
                )
                // end of after
            } else if (type == "before") {
                /**
                 * 화면 분석
                 * 이전 화면 집계
                 */

                val matchQuery = D()
                    .append(
                        P.stz,
                        D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
                    )
                    .append(P.ai, D("\$in", appIdInObjIds))
                    .append(P.vhi, D("\$eq", viewId.toLong()))

                if (versionList.isNotEmpty()) {
                    matchQuery.append(P.av, D("\$in", versionList))
                }

                IndicatorByEachFlow.COLLECTION_NAME_PT24H to listOf(
                    D("\$match", matchQuery),

                    D(
                        "\$group", D()
                            .append(
                                "_id", D()
                                    .append(P.bvhi, "\$${P.bvhi}")
                            )
                            .append(P.mco, D("\$sum", "\$${P.mco}"))
                    ),

                    D(
                        "\$group", D()
                            .append("_id", 0)
                            .append(
                                "data", D()
                                    .append(
                                        "\$push", D()
                                            .append(P.bvhi, "\$_id.${P.bvhi}")
                                            .append(P.mco, "\$${P.mco}")
                                    )
                            )
                            .append("total_${P.mco}", D("\$sum", "\$${P.mco}"))
                    ),

                    D("\$unwind", "\$data"),

                    D(
                        "\$project", D()
                            .append("_id", 0)
                            .append(P.bvhi, "\$data.${P.bvhi}")
                            .append(P.mco, "\$data.${P.mco}")
                            .append(
                                "ratio",
                                D(
                                    "\$multiply",
                                    listOf(D("\$divide", listOf("\$data.${P.mco}", "\$total_${P.mco}")), 100)
                                )
                            )
                    ),

                    D(
                        "\$lookup", D()
                            .append("from", ViewList.COLLECTION_NAME_PT24H)
                            .append("let", D(P.vhi, "\$${P.bvhi}"))
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
                        "\$project", D()
                            .append(P.bvhiK, "\$${P.bvhi}")
                            .append(P.mcoK, "\$${P.mco}")
                            .append("ratio", "\$ratio")
                            .append("view", "\$view")
                    ),

                    D("\$sort", D(sortField, sortValue)),

                    D("\$skip", skip),
                    D("\$limit", limit)
                )
                // end of before
            } else {
                "" to listOf()
            }

//        println(Util.toPretty(query))

            Flux
                .from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError { log.info(Util.toPretty(query)) }
                .map {
//            println(">>\n${it.first()}\n${it.last()}\n${it.count()}")
//            println("Res: ${Util.toPretty(it)}")
                    Status.status200Ok(it)
                }
            // end of getAppIdList()
        }
    } // end of get()


    fun getTargetFlowOfDepth(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")

        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val versionList = req.splitQueryOrDefault("version_list", "")

        val fromViewId = req.getQueryOrDefault("from_view_id", "")
        val toViewId = req.getQueryOrDefault("to_view_id", "")
        val depth = req.getQueryOrDefault("depth", 1)

        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(limit, "limit").max(100)
            .new(fromViewId, "from_view_id").required()
            .new(toViewId, "to_view_id").required()
            .new(depth, "depth").min(1)
        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val flowServ = FlowServiceGetTarget()
        val mongoResStream = flowServ.getMongoResStreamTargetFlowsOfDepth(
            appIdStream,
            appIdList, versionList, fromDate, toDate, fromViewId, toViewId,
            depth
        )
        val respStream = mongoResStream.map { result ->
//      println(result)
            Status.status200Ok(result)
        }
        return respStream
    }

    fun getNodesOfEachFlowGraph(req: SafeRequest): Mono<Map<String, Any>> {
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val flowServ = FlowServiceGetGraph()
        val mongoResStream = flowServ.getMongoResStreamGraphNodes(
            appIdStream,
            appIdList, versionList, fromDate, toDate
        )
        val respStream = mongoResStream.map { result ->
            Status.status200Ok(result)
        }
        return respStream
    }

    fun getEdgesOfEachFlowGraph(req: SafeRequest): Mono<Map<String, Any>> {
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val flowServ = FlowServiceGetGraph()
        val mongoResStream = flowServ.getMongoResStreamGraphEdges(
            appIdStream,
            appIdList, versionList, fromDate, toDate
        )
        val respStream = mongoResStream.map { result ->
            Status.status200Ok(result)
        }
        return respStream
    }

    /**
     * @author cjh
     * @comment 21.11.18 yj
     * @sample [GET {{localhost}}/v3/flow/{ids}/count?app_id_list=e6c101f020e1018b5ba17cdbe32ade2d679b44bc&from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z]
     * @return data=[...]
     * {
     *   "view_hash_id": 393226868,
     *   "after_view_hash_id": -784699524,
     *   "alias": null,
     *   "view_id": "com.hdsec.android.mainlib.SmartActivitySO0300P1.xmf",
     *   "after_alias": null,
     *   "after_view_id": "com.hdsec.android.mainlib.SmartActivityEO231012",
     *   "move_count": 58,
     *   "session_count": 29,
     *   "average": 2.0,
     *   "total_average": 1.1776027996500438
     * },
     */
    fun count(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val viewHashId = req.getQueryOrDefault(P.vhiK, "")
        val afterViewHashId = req.getQueryOrDefault(P.avhiK, "")
        val type = req.getQueryOrDefault("type", "each")
        val searchListStart = req.getQueryOrDefault("search_list_start", "")
        val searchListEnd = req.getQueryOrDefault("search_list_end", "")

        val toDate = req.getQueryOrDefault("to_date", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)
        val sortField = req.getQueryOrDefault("sort_key", "average")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val fieldList = req.splitQueryOrDefault("field_list", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(limit, "limit").max(100)
            .new(type, "type").required(listOf("each", "repeat"))

        if (type == "each") vali.new(sortField, "sort_key").required(listOf("average", P.mcoK, P.scoK))

        if (type == "repeat") {
            vali
                .new(viewHashId, P.vhiK).required()
                .new(afterViewHashId, P.avhiK).required()
        }

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        // 개별 경로 분석 전체 리스트

        return Mono
            .zip(
                AppService.getAppIdList(P.uiK, req.getUserId()),
                ViewList.get(
                    req.getNew(
                        mapOf("field_list" to listOf("vhi,alias")) +
                                if (searchListStart.isNotEmpty()) mapOf(
                                    "search_field" to listOf("alias"),
                                    "search_list" to listOf(searchListStart)
                                )
                                else mapOf(), mapOf()
                    )
                ), // param "search_list" 가 없으면 빈 모노
                ViewList.get(
                    req.getNew(
                        mapOf("field_list" to listOf("vhi,alias")) +
                                if (searchListEnd.isNotEmpty()) mapOf(
                                    "search_field" to listOf("alias"),
                                    "search_list" to listOf(searchListEnd)
                                )
                                else mapOf(), mapOf()
                    )
                ) // param "search_list" 가 없으면 빈 모노
            )
            .flatMap { result ->
                val ownedAppIdList = result.t1
                val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
                    if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
                }
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

                val viewIdStart =
                    result.t2.let { if (it.isNotEmpty() && it["status"] == 200) it["data"] as List<Map<String, Any>> else listOf() }
                        .map { it["vhi"] as Int }.toSet()
                val viewIdEnd =
                    result.t3.let { if (it.isNotEmpty() && it["status"] == 200) it["data"] as List<Map<String, Any>> else listOf() }
                        .map { it["vhi"] as Int }.toSet()

                val (coll, query) = if (type == "each") {

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

                    if (viewIdStart.isNotEmpty() || viewIdEnd.isNotEmpty()) {
                        val list = mutableListOf<D>()
                        if (viewIdStart.isNotEmpty()) list.add(D(P.vhi, listOf("\$in", viewIdStart)))
                        if (viewIdEnd.isNotEmpty()) list.add(D(P.avhi, listOf("\$in", viewIdEnd)))
                        matchQuery.append("\$or", list)
                    }

                    IndicatorByEachFlow.COLLECTION_NAME_PT24H to listOf(
                        D("\$match", matchQuery),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.vhi, "\$${P.vhi}")
                                        .append(P.avhi, "\$${P.avhi}")
                                )
                                .append(P.mco, D("\$sum", "\$${P.mco}"))
                                .append(P.sco, D("\$sum", "\$${P.sco}"))
                                .append(P.ai, D("\$max", "\$${P.ai}"))
                                .append(P.av, D("\$max", "\$${P.av}"))
                        ),

                        D(
                            "\$group", D()
                                .append("_id", D())
                                .append(
                                    "data", D(
                                        "\$push", D()
                                            .append(P.vhi, "\$_id.${P.vhi}")
                                            .append(P.avhi, "\$_id.${P.avhi}")
                                            .append(P.mco, "\$${P.mco}")
                                            .append(P.sco, "\$${P.sco}")
                                            .append(P.ai, "\$${P.ai}")
                                            .append(P.av, "\$${P.av}")
                                    )
                                )
                                .append("total_${P.mco}", D("\$sum", "\$${P.mco}"))
                                .append("total_${P.sco}", D("\$sum", "\$${P.sco}"))
                        ),

                        D("\$unwind", "\$data"),

                        D(
                            "\$lookup", D()
                                .append("from", ViewList.COLLECTION_NAME_PT24H)
                                .append(
                                    "let", D(P.vhi, "\$data.${P.vhi}")
                                        .append(P.ai, "\$data.${P.ai}")
                                        .append(P.av, "\$data.${P.av}")
                                )
                                .append(
                                    "pipeline", listOf(
                                        D(
                                            "\$match", D(
                                                "\$expr",
                                                D(
                                                    "\$and", listOf(
                                                        D("\$eq", listOf("\$${P.ai}", "\$\$${P.ai}")),
                                                        D("\$eq", listOf("\$${P.av}", "\$\$${P.av}")),
                                                        D("\$eq", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                                .append("as", "view")
                        ),
                        D(
                            "\$lookup", D()
                                .append("from", ViewList.COLLECTION_NAME_PT24H)
                                .append(
                                    "let", D(P.avhi, "\$data.${P.avhi}")
                                        .append(P.ai, "\$data.${P.ai}")
                                        .append(P.av, "\$data.${P.av}")
                                )
                                .append(
                                    "pipeline", listOf(
                                        D(
                                            "\$match", D(
                                                "\$expr",
                                                D(
                                                    "\$and", listOf(
                                                        D("\$eq", listOf("\$${P.ai}", "\$\$${P.ai}")),
                                                        D("\$eq", listOf("\$${P.av}", "\$\$${P.av}")),
                                                        D("\$eq", listOf("\$${P.vhi}", "\$\$${P.avhi}"))
                                                    )
                                                )
                                            )
                                        ),
                                    )
                                )
                                .append("as", "after_view")
                        ),

                        D(
                            "\$project", D()
                                .append("_id", 0)
                                .append(P.vhiK, "\$data.${P.vhi}")
                                .append(P.avhiK, "\$data.${P.avhi}")
                                .append("alias", D("\$arrayElemAt", listOf("\$view.alias", 0)))
                                .append(P.viK, D("\$arrayElemAt", listOf("\$view.${P.vi}", 0)))
                                .append("after_alias", D("\$arrayElemAt", listOf("\$after_view.alias", 0)))
                                .append("after_${P.viK}", D("\$arrayElemAt", listOf("\$after_view.${P.vi}", 0)))
                                .append(P.mcoK, "\$data.${P.mco}")
                                .append(P.scoK, "\$data.${P.sco}")
                                .append("average", D("\$divide", listOf("\$data.${P.mco}", "\$data.${P.sco}")))
                                .append("total_average", D("\$divide", listOf("\$total_${P.mco}", "\$total_${P.sco}")))
                        ),

                        D("\$sort", D(sortField, sortValue)),

                        D("\$skip", skip),

                        D("\$limit", limit),
                    )
                    // end of each
                } else if (type == "repeat") {
                    // 동일 경로 분석 A to B

                    val matchQuery = D()
                        .append(
                            P.stz,
                            D("\$gte", Date.from(Instant.parse(fromDate))).append(
                                "\$lte",
                                Date.from(Instant.parse(toDate))
                            )
                        )
                        .append(P.ai, D("\$in", appIdInObjIds))
                        .append(P.vhi, D("\$eq", viewHashId.toLong()))
                        .append(P.avhi, D("\$eq", afterViewHashId.toLong()))

                    if (versionList.isNotEmpty()) {
                        matchQuery.append(P.av, D("\$in", versionList))
                    }

                    IndicatorByEachFlow.COLLECTION_NAME_PT24H to listOf(
                        D("\$match", matchQuery),

                        D(
                            "\$group", D()
                                .append(
                                    "_id", D()
                                        .append(P.vhi, "\$${P.vhi}")
                                        .append(P.avhi, "\$${P.avhi}")
                                        .append(P.rco, "\$${P.rco}")
                                )
                                .append(P.sco, D("\$sum", "\$${P.sco}"))
                        ),
                        D(
                            "\$project", D()
                                .append("_id", 0)
                                .append(P.vhiK, "\$_id.${P.vhi}")
                                .append(P.avhiK, "\$_id.${P.avhi}")
                                .append(P.rcoK, "\$_id.${P.rco}")
                                .append(P.scoK, "\$${P.sco}")
                        ),
                        D("\$sort", D(P.rcoK, 1)),
                        D("\$skip", skip),
                        D("\$limit", limit),
                    )
                } else {
                    "" to listOf(D())
                }

//        println(Util.toPretty(query))

                Flux
                    .from(MongodbUtil.getCollection(coll).aggregate(query))
                    .collectList()
                    .doOnError { log.info(Util.toPretty(query)) }
                    .map {
                        Status.status200Ok(it)
                    }

                // end of getAppIdList()
            }
        // end of count()
    }


}
