package io.userhabit.polaris.service.flow

import io.userhabit.batch.indicators.IndicatorAllByView
import io.userhabit.batch.indicators.IndicatorByEachFlow
import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D

class FlowServiceGetGraph {

    private val log = Loggers.getLogger(this.javaClass)
    private val _TEMP_FIELDNAME_VIEWINFO = "view_info"

    /**
     * @return
     * [
     *    {
     *        "vhi": 422916072,
     *        "dwell_time": 0,
     *        "session": 32,
     *        "view_info": {
     *            "vi": "Main.대분류-0.소분 류-A화면",
     *            "alias": "(Inttr6oActivity)"
     *        }
     *    },
     *    {...},
     * ]
     */
    fun getMongoResStreamGraphNodes(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        fromDate: String,
        toDate: String,
    ): Mono<List<D>> {

        return appIdStream.flatMap { ownedAppIdList ->
            val safeAppList: List<String> = this._getSafeAppList(ownedAppIdList, appIdList)

            val (coll, query) = this._getMongoQueryGraphNodes(
                fromDate, toDate,
                safeAppList, versionList,
            )

            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(query))
                }
        }
    }

    /**
     *
     * return query example:
     *
     * db.indicator_by_each_flow_pt24h.aggregate([
     *   {
     *     $match: {
     *       "stz": {
     *         "$gte": ISODate("2022-03-04"),
     *         "$lte": ISODate("2022-03-18")
     *       },
     *       ai: { $in: [ObjectId("000000000000000000000004")] },
     *       av: { $in: ["1.0.1"] },
     *     }
     *   },
     *   {
     *     $group: {
     *       _id: {
     *         // vhi: "$vhi",
     *         // avhi: "$avhi"
     *         vhi: "$vhi",
     *         bvhi: "$bvhi"
     *       },
     *       move: {
     *         $sum: "$mco"
     *       },
     *       session: {
     *         $sum: "$sco"
     *       }
     *     }
     *   },
     *   {
     *     $project: {
     *       _id: 0,
     *       from: "$_id.bvhi",
     *       to: "$_id.vhi",
     *       move: "$move",
     *       session: "$session"
     *
     *     }
     *   }
     * ])
     *
     *
     */
    fun getMongoResStreamGraphEdges(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        fromDate: String,
        toDate: String,
    ): Mono<List<D>> {
        return appIdStream.flatMap { ownedAppIdList ->
            val safeAppList: List<String> = this._getSafeAppList(ownedAppIdList, appIdList)
            val (coll, query) = this._getMongoQueryGraphEdges(
                fromDate, toDate,
                safeAppList, versionList,
            )

            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(query))
                }
        }
    }

    private fun _getSafeAppList(ownedAppIdList: List<String>, appIdList: List<String>): List<String> {
        return if (appIdList.isEmpty()) {
            ownedAppIdList
        } else {
            appIdList.filter { appId ->
                ownedAppIdList.contains(appId)
            }
        }
    }

    /**
     *
     * return query example:
     *
     * db.indicator_all_by_view_pt24h.aggregate([
     *     {
     *         $match:{
     *             "stz": {
     *                 "$gte": ISODate("2022-03-04"),
     *                 "$lte": ISODate("2022-03-18")
     *             },
     *             ai: {$in: [ObjectId("000000000000000000000004")]},
     *             av: {$in: ["1.0.1"]},
     *
     *
     *         }
     *     },
     *     {
     *         $group:{
     *             _id: {
     *                 vhi: "$vhi",
     *             },
     *             dwell_time: {
     *                 $sum: "dt"
     *             },
     *             session: {
     *                 $sum: "$sco"
     *             }
     *         }
     *     },
     *     {
     *         $project:{
     *             _id: 0,
     *             vhi: "$_id.vhi",
     *             dwell_time: "$dwell_time",
     *             session: "$session"
     *
     *         }
     *     }
     * ])
     */
    private fun _getMongoQueryGraphNodes(
        fromDate: String,
        toDate: String,
        appIdList: List<String>,
        versionList: List<String>,
    ): Pair<String, List<D>> {

        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return IndicatorAllByView.COLLECTION_NAME_PT24H to listOf(
            this._getMatchQuery(fromDate, toDate, appIdInObjIds, versionList),
            this._getGroupByVhiQuery(),
            this._getLookupQueryForViewInfo(appIdInObjIds, versionList),
            this._getProjectQueryForNodes(),
        )
    }

    private fun _getMongoQueryGraphEdges(
        fromDate: String,
        toDate: String,
        appIdList: List<String>,
        versionList: List<String>,
    ): Pair<String, List<D>> {

        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return IndicatorByEachFlow.COLLECTION_NAME_PT24H to listOf(
            this._getMatchQuery(fromDate, toDate, appIdInObjIds, versionList),
            this._getGroupByVhiBvhi(),
            this._getProjectQueryForEdges(),
        )
    }

    /**
     * {
     *     $match:{
     *         "stz": {
     *             "$gte": ISODate("2022-03-04"),
     *             "$lte": ISODate("2022-03-18")
     *         },
     *         ai: {$in: [ObjectId("000000000000000000000004")]},
     *         av: {$in: ["1.0.1"]},
     *     }
     * },
     */
    private fun _getMatchQuery(
        fromDate: String,
        toDate: String,
        appIdInObjIds: List<ObjectId>,
        versionList: List<String>,
    ): D {

        val matchQuery = D(
            "stz", D(
            "\$gte", Date.from(Instant.parse(fromDate))
        )
            .append("\$lt", Date.from(Instant.parse(toDate)))
        )
            .append("ai", D("\$in", appIdInObjIds))

        if (versionList.isNotEmpty()) {
            matchQuery.append("av", D("\$in", versionList))
        }

        return D("\$match", matchQuery)
    }

    /**
     * {
     *     $group:{
     *         _id: {
     *             vhi: "$vhi",
     *         },
     *         dwell_time: {
     *             $sum: "dt"
     *         },
     *         session: {
     *             $sum: "$sco"
     *         }
     *     }
     * },
     */
    private fun _getGroupByVhiQuery(): D {
        return D(
            "\$group",
            D(
                "_id", D("vhi", "\$vhi")
            )
                .append("dwell_time", D("\$sum", "dt"))
                .append("session", D("\$sum", "\$sco"))
        )
    }

    /**
     * {"$lookup":{
     *   "from":"view_list_pt24h",
     *   "let": {"vhi": "$_id.vhi"},
     *   "pipeline": [
     *     { "$match":
     *       { "$expr":
     *         { "$and": [
     *           { "$eq": [ "$vhi",  "$$vhi" ] },
     *           { "$in": [ "$ai",  [ObjectId("000000000000000000000004")] ] },
     *           { "$in": [ "$av",  ["1.0.1"] ] }
     *         ]}
     *       }
     *     },
     *     {
     *         "$project" : {
     *             "_id": 0,
     *             "alias": 1,
     *             "vi": 1,
     *         }
     *     }
     *   ],
     *   "as":"view_info"
     * }},
     */
    private fun _getLookupQueryForViewInfo(
        appIdInObjIds: List<ObjectId>,
        versionList: List<String>,
    ): D {
        val matchCond = mutableListOf(
            D("\$eq", listOf("\$vhi", "\$\$vhi")),
            D("\$in", listOf("\$ai", appIdInObjIds)),
        )
        if (versionList.isNotEmpty()) {
            matchCond += D("\$in", listOf("\$av", versionList))
        }
        return D(
            "\$lookup",
            D(
                "from", ViewList.COLLECTION_NAME_PT24H
            ).append("let", D("vhi", "\$_id.vhi"))
                .append("pipeline", listOf(
                    D("\$match",
                        D("\$expr", D("\$and", matchCond))
                    ),
                    D("\$project",
                        D("_id", 0)
                            .append("alias", 1)
                            .append("vi", 1)
                    )
                ))
                .append("as", _TEMP_FIELDNAME_VIEWINFO)
        )
    }

    /**
     * {
     *     $project:{
     *         _id: 0,
     *         vhi: "$_id.vhi",
     *         dwell_time: "$dwell_time",
     *         session: "$session",
     *         view_info: {$arrayElemAt: ["$view_info", 0]}
     *     }
     * }
     */
    private fun _getProjectQueryForNodes(): D {
        return D(
            "\$project",
            D("_id", 0)
                .append("vhi", "\$_id.vhi")
                .append("dwell_time", "\$dwell_time")
                .append("session", "\$session")
                .append("view_info", D("\$arrayElemAt", listOf("\$${_TEMP_FIELDNAME_VIEWINFO}", 0)))
        )
    }

    /**
     * {
     *   $group: {
     *     _id: {
     *       vhi: "$vhi",
     *       bvhi: "$bvhi"
     *     },
     *     move: {
     *       $sum: "$mco"
     *     },
     *     session: {
     *       $sum: "$sco"
     *     }
     *   }
     * },
     */
    private fun _getGroupByVhiBvhi(): D {
        return D("\$group",
            D(
                "_id",
                D("vhi", "\$vhi")
                    .append("bvhi", "\$bvhi")
            )
                .append("move", D("\$sum", "\$mco"))
                .append("session", D("\$sum", "\$sco"))
        )
    }

    /**
     * {
     *   $project: {
     *     _id: 0,
     *     from: "$_id.bvhi",
     *     to: "$_id.vhi",
     *     move: "$move",
     *     session: "$session"
     *   }
     * }
     */
    private fun _getProjectQueryForEdges(): D {
        return D(
            "\$project",
            D("_id", 0)
                .append("from", "\$_id.bvhi")
                .append("to", "\$_id.vhi")
                .append("move", "\$move")
                .append("session", "\$session")
        )
    }
}