package io.userhabit.polaris.service.heatmap


import io.userhabit.batch.HeatmapByScrollViewBatch
import io.userhabit.batch.indicators.*
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.EventType
import io.userhabit.polaris.Protocol
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D

class HeatmapServiceScrollView {

    private val log = Loggers.getLogger(this.javaClass)

    companion object {
        const val TYPE_SCROLLVIEW = "scroll_view"
        const val TYPE_REACHRATE = "reach_rate"

    }

    /**
     * @return
     * [
     *    {
     *      "spx" : 90,
     *      "spy" : 100,
     *      "count" : 1
     *    },
     *    {...},
     * ]
     */
    fun getMongoResStreamScrollView(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        fromDate: String,
        toDate: String,
        viewList: List<String>,
        type: String,
        actionType: String? = null
    ): Mono<List<D>> {

        return appIdStream.flatMap { ownedAppIdList ->
            val safeAppList: List<String> = this._getSafeAppList(ownedAppIdList, appIdList)

            val (coll, query) = this._getMongoQueryScrollView(
                fromDate, toDate,
                safeAppList, versionList, viewList, type, actionType
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
     * db.indicator_heatmap_by_scroll_view_pt24h.aggregate(
     * [
     *   {
     *     "$match": {
     *       "stz": {
     *         "$gte": ISODate('2022-03-08'),
     *         "$lte": ISODate('2022-03-17'),
     *       },
     *       "ai": {
     *         "$in": [
     *           ObjectId("000000000000000000000004")
     *         ]
     *       },
     *       // --------------------------------
     *       // for scroll_view
     *       // --------------------------------
     *       "t": {
     *         "$eq": 1101
     *       },
     *       "av": {
     *         "$in": [
     *           "1.0.1"
     *         ]
     *       },
     *       "vhi": {
     *         "$in": [
     *           422916072
     *         ]
     *       },
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": {
     *         "ai": "$ai",
     *         "vhi": "$vhi",
     *         "svi": "$svi",
     *         "spx": "$spx",
     *         "spy": "$spy"
     *       },
     *       "count": {
     *         "$sum": "$count"
     *       }
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": {
     *         "ai": "$_id.ai",
     *         "svi": "$_id.svi",
     *         "vhi": "$_id.vhi"
     *       },
     *       "data": {
     *         "$push": {
     *           "spx": "$_id.spx",
     *           "spy": "$_id.spy",
     *           "count": "$count"
     *         }
     *       }
     *     }
     *   },
     *   {
     *     "$lookup": {
     *       "from": "view_list_pt24h",
     *       "let": {
     *         "ai": "$_id.ai",
     *         "vhi": "$_id.vhi"
     *       },
     *       "pipeline": [
     *         {
     *           "$match": {
     *             "$expr": {
     *               "$and": [
     *                 {"$eq": [ "$ai", "$$ai" ]},
     *                 {"$eq": ["$vhi","$$vhi"]},
     *                 {"$in": [ "$av", [ "1.0.1" ]]}
     *               ]
     *             }
     *           }
     *         },
     *         {
     *           "$sort": {
     *             "av": -1,
     *           }
     *         },
     *         {
     *           "$limit": 1
     *         }
     *       ],
     *       "as": "view"
     *     }
     *   },
     *   {
     *     "$unwind": "$view"
     *   },
     *   {
     *     "$project": {
     *       "_id": 0,
     *       "dw": 720,
     *       "dh": 1280,
     *       "app_id": "$_id.ai",
     *       "view_hash_id": "$_id.vhi",
     *       "scroll_view_id": "$_id.svi",
     *       "view_id": "$view.vi",
     *       "alias": "$view.alias",
     *       "favorite": "$view.favorite",
     *       "data": "$data"
     *     }
     *   }
     * ])
     */
    private fun _getMongoQueryScrollView(
        fromDate: String,
        toDate: String,
        appIdList: List<String>,
        versionList: List<String>,
        viewIdList: List<String>,
        type: String,
        actionType: String?
    ): Pair<String, List<D>> {

        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
        val viewListInInt = viewIdList.map { it.toInt() }

        return IndicatorHeatmapByScrollView.COLLECTION_NAME_PT24H to listOf(
            this._getMatchQuery(fromDate, toDate, appIdInObjIds, versionList, viewListInInt, type, actionType),
            this._getGroupByVhiXYQuery(),
            this._getGroupByVhiSviQuery(),
            this._getLookupViewQuery(versionList),
            D("\$unwind", "\$view"),
            this._getProjectQueryForNodes()
        )
    }

    /**
     * {
     *   "$match": {
     *     "stz": {
     *       "$gte": ISODate('2022-03-08'),
     *       "$lte": ISODate('2022-03-17'),
     *     },
     *     "ai": {
     *       "$in": [
     *         ObjectId("000000000000000000000004")
     *       ]
     *     },
     *     "t": {
     *       "$in": [1101]
     *     },
     *     "av": {
     *       "$in": [
     *         "1.0.1"
     *       ]
     *     },
     *     "vhi": {
     *       "$in": [
     *         422916072
     *       ]
     *     },
     *   }
     * },
     */
    private fun _getMatchQuery(
        fromDate: String,
        toDate: String,
        appIdList: List<ObjectId>,
        versionList: List<String>,
        viewIdList: List<Int>,
        type: String,
        actionType: String?
    ): D {
        val matchQuery = D()
            .append(
                Protocol.stz,
                D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
            )
            .append(Protocol.ai, D("\$in", appIdList))

        if (type == TYPE_SCROLLVIEW) {
            val eventTypeList = HeatmapServiceActionType.eventType[actionType]
                ?: HeatmapServiceActionType.eventType[HeatmapServiceActionType.TAP]
            matchQuery.append(Protocol.t, D("\$in", eventTypeList))
        }

        if (versionList.isNotEmpty()) {
            matchQuery.append(Protocol.av, D("\$in", versionList))
        }

        if (viewIdList.isNotEmpty()) {
            matchQuery.append(Protocol.vhi, D("\$in", viewIdList))
        }
        return D("\$match", matchQuery)
    }

    /**
     * {
     *   "$group": {
     *     "_id": {
     *       "ai": "$ai",
     *       "vhi": "$vhi",
     *       "svi": "$svi",
     *       "spx": "$spx",
     *       "spy": "$spy"
     *     },
     *     "count": {
     *       "$sum": "$count"
     *     }
     *   }
     * }
     */
    private fun _getGroupByVhiXYQuery(): D {
        return D(
            "\$group", D()
                .append(
                    "_id", D()
                        .append(Protocol.ai, "\$${Protocol.ai}")
                        .append(Protocol.vhi, "\$${Protocol.vhi}")
                        .append(Protocol.svi, "\$${Protocol.svi}")
                        .append("spx", "\$spx")
                        .append("spy", "\$spy")
                )
                .append("count", D("\$sum", "\$count"))
        )
    }

    /**
     * {
     *   "$group": {
     *     "_id": {
     *       "ai": "$_id.ai",
     *       "svi": "$_id.svi",
     *       "vhi": "$_id.vhi"
     *     },
     *     "data": {
     *       "$push": {
     *         "spx": "$_id.spx",
     *         "spy": "$_id.spy",
     *         "count": "$count"
     *       }
     *     }
     *   }
     * },
     */
    private fun _getGroupByVhiSviQuery(): D {
        return D(
            "\$group", D()
                .append(
                    "_id", D()
                        .append(Protocol.ai, "\$_id.${Protocol.ai}")
                        .append(Protocol.svi, "\$_id.${Protocol.svi}")
                        .append(Protocol.vhi, "\$_id.${Protocol.vhi}")
                )
                .append(
                    "data", D(
                        "\$push", D()
                            .append("spx", "\$_id.spx")
                            .append("spy", "\$_id.spy")
                            .append("count", "\$count")
                    )
                )
        )
    }

    /**
     * {
     *   "$lookup": {
     *     "from": "view_list_pt24h",
     *     "let": {
     *       "ai": "$_id.ai",
     *       "vhi": "$_id.vhi"
     *     },
     *     "pipeline": [
     *       {
     *         "$match": {
     *           "$expr": {
     *             "$and": [
     *               {"$eq": [ "$ai", "$$ai" ]},
     *               {"$eq": ["$vhi","$$vhi"]},
     *               {"$in": [ "$av", [ "1.0.1" ]]}
     *             ]
     *           }
     *         }
     *       },
     *       {
     *         "$sort": {
     *           "av": -1,
     *         }
     *       },
     *       {
     *         "$limit": 1
     *       }
     *     ],
     *     "as": "view"
     *   }
     * },
     */
    private fun _getLookupViewQuery(versionList: List<String>): D {
        val matchCond = mutableListOf(
            D("\$eq", listOf("\$${Protocol.ai}", "\$\$${Protocol.ai}")),
            D("\$eq", listOf("\$${Protocol.vhi}", "\$\$${Protocol.vhi}")),
        )
        if (versionList.isNotEmpty()) {
            matchCond += D("\$in", listOf("\$${Protocol.av}", versionList))
        }
        return D(
            "\$lookup", D()
                .append("from", ViewList.COLLECTION_NAME_PT24H)
                .append(
                    "let",
                    D(Protocol.ai, "\$_id.${Protocol.ai}")
                        .append(Protocol.vhi, "\$_id.${Protocol.vhi}")
                )
                .append(
                    "pipeline", listOf(
                        D("\$match", D("\$expr", D("\$and", matchCond))),
                        D("\$sort", D("${Protocol.av}", -1)),
                        D("\$limit", 1)
                    )
                )
                .append("as", "view")
        )
    }

    /**
     * {
     *   "$project": {
     *     "_id": 0,
     *     "dw": 720,
     *     "dh": 1280,
     *     "app_id": "$_id.ai",
     *     "view_hash_id": "$_id.vhi",
     *     "scroll_view_id": "$_id.svi",
     *     "view_id": "$view.vi",
     *     "alias": "$view.alias",
     *     "favorite": "$view.favorite",
     *     "data": "$data"
     *   }
     * }
     */
    private fun _getProjectQueryForNodes(): D {

        return D(
            "\$project", D()
                .append("_id", 0)
                .append("dw", HeatmapByScrollViewBatch.REPRESENTWIDTH)
                .append("dh", HeatmapByScrollViewBatch.REPRESENTHEIGHT)
                .append(Protocol.aiK, "\$_id.${Protocol.ai}")
                .append(Protocol.vhiK, "\$_id.${Protocol.vhi}")
                .append(Protocol.sviK, "\$_id.${Protocol.svi}")
                .append(Protocol.viK, "\$view.${Protocol.vi}")
                .append("alias", "\$view.alias")
                .append("favorite", "\$view.favorite")
                .append("data", "\$data")
        )
    }
}
