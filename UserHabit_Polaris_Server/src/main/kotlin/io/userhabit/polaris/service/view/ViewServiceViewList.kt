package io.userhabit.polaris.service.view

import io.userhabit.batch.indicators.IndicatorAllByView
import io.userhabit.batch.indicators.IndicatorDeviceByView
import io.userhabit.batch.indicators.IndicatorEventByView
import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.EventType
import io.userhabit.polaris.Protocol
import org.bson.types.ObjectId
import org.bson.Document as D
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers

class ViewServiceViewList {
    companion object {
        const val SORTKEY_SESSION = "session"
        const val SORTKEY_SCREENVIEW = "screen_view"
        const val SORTKEY_SCREENVIEWTIME = "screen_view_time"
        const val SORTKEY_SCREENVIEWACTION = "screen_view_action"
        const val SORTKEY_SCREENVIEWACTIONNORESP = "unresponse"
        const val SORTKEY_DAU = "dau"
        const val SORTKEY_CRASH = "crash"
        const val SORTKEY_APPEND = "app_end"
    }

    private val log = Loggers.getLogger(this.javaClass)
    private val _TMPFIELD_TOTALVAL = "count"
    private val _VHI_SESSIONTSTART = 74180013
    private val _VHI_SESSIONTEND = -1671032428

    private val _SORTFIELD = mapOf(
        SORTKEY_SESSION to "sco",
        SORTKEY_SCREENVIEW to "vco",
        SORTKEY_SCREENVIEWTIME to "dt",
        SORTKEY_SCREENVIEWACTION to "eco",
        SORTKEY_SCREENVIEWACTIONNORESP to "eco",
        SORTKEY_DAU to "dco",
        SORTKEY_CRASH to "cco",
        SORTKEY_APPEND to "tco"
    )
    private val _COLLFOR = mapOf(
        SORTKEY_SESSION to IndicatorAllByView.COLLECTION_NAME_PT24H,
        SORTKEY_SCREENVIEW to IndicatorAllByView.COLLECTION_NAME_PT24H,
        SORTKEY_SCREENVIEWTIME to IndicatorAllByView.COLLECTION_NAME_PT24H,
        SORTKEY_SCREENVIEWACTION to IndicatorEventByView.COLLECTION_NAME_PT24H,
        SORTKEY_SCREENVIEWACTIONNORESP to IndicatorEventByView.COLLECTION_NAME_PT24H,
        SORTKEY_DAU to IndicatorDeviceByView.COLLECTION_NAME_PT24H,
        SORTKEY_CRASH to IndicatorAllByView.COLLECTION_NAME_PT24H,
        SORTKEY_APPEND to IndicatorAllByView.COLLECTION_NAME_PT24H
    )

    fun getMongoResStreamViewList(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        sortKey: String,
        sortOrder: Int,
        offset: Int,
        limit: Int
    ): Mono<List<D>> {
        return appIdStream.flatMap { ownedAppIdList ->

            val safeAppList = this._getSafeAppList(ownedAppIdList, appIdList)
            val (coll, query) = this._getMongoQueryViewList(safeAppList, versionList, sortKey, sortOrder, offset, limit)

            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(query))
                }
        }
    }

    fun getMongoResStreamViewCount(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        sortKey: String,
    ): Mono<List<D>> {
        return appIdStream.flatMap { ownedAppIdList ->

            val safeAppList = this._getSafeAppList(ownedAppIdList, appIdList)
            val (coll, query) = this._getMongoQueryViewCount(safeAppList, versionList, sortKey)

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
     * db.indicator_all_by_view_pt24h.aggregate(
     * [
     *   {
     *     "$match": {
     *       "ai": {
     *         "$in": [
     *           ObjectId("000000000000000000000004")
     *         ]
     *       },
     *       "av": {
     *         "$in": ["1.0.1"]
     *       },
     *       "vhi": {
     *         "$nin": [
     *           74180013,
     *           -1671032428
     *         ]
     *       }
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": {
     *         "vhi": "$vhi"
     *       },
     *       "vhi": {$first: "$vhi"},
     *       "total": {
     *         "$sum": "$sco"
     *       }
     *     }
     *   },
     *   {
     *       $project : {
     *           _id: 0
     *       }
     *   },
     *   {
     *     "$sort": {
     *       "total": -1
     *     }
     *   },
     *   {
     *     "$skip": 0
     *   },
     *   {
     *     "$limit": 100
     *   },
     *   {
     *     "$lookup": {
     *       "from": "view_list_pt24h",
     *       "let": {
     *         "vhi": "$vhi"
     *       },
     *       "pipeline": [
     *         {
     *           "$match": {
     *             "$expr": {
     *               "$and": [
     *                 {"$eq": ["$vhi","$$vhi"]},
     *                 {"$in":["$ai", [ObjectId("000000000000000000000004")]]},
     *                 {"$in":["$av", ["1.0.1"]]}
     *               ]
     *             }
     *           }
     *         },
     *         {
     *             $project:{
     *                 _id: 0,
     *             }
     *         }
     *       ],
     *       "as": "view"
     *     }
     *   },
     * ])
     */
    private fun _getMongoQueryViewList(
        appIdList: List<String>,
        versions: List<String>,
        sortKey: String,
        sortOrder: Int,
        offset: Int,
        limit: Int
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
        val coll = _COLLFOR[sortKey] ?: IndicatorAllByView.COLLECTION_NAME_PT24H

        return coll to listOf(
            this._getPipeMatchQuery(appIdInObjIds, versions, sortKey),
            this._getPipeGroupByVhi(sortKey),
            D("\$project", D("_id", 0)),
            this._getPipeSortBy(sortKey, sortOrder),
            D("\$skip", offset),
            D("\$limit", limit),
            this._getPipeLookupViewInfo(appIdInObjIds, versions)
        )
    }

    /**
     * db.indicator_all_by_view_pt24h.aggregate(
     * [
     *   {
     *     "$match": {
     *       "ai": {
     *         "$in": [
     *           ObjectId("000000000000000000000004")
     *         ]
     *       },
     *       "av": {
     *         "$in": ["1.0.1"]
     *       },
     *       "vhi": {
     *         "$nin": [
     *           74180013,
     *           -1671032428
     *         ]
     *       }
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": {
     *         "vhi": "$vhi"
     *       },
     *       "vhi": {$first: "$vhi"},
     *       "total": {
     *         "$sum": "$sco"
     *       }
     *     }
     *   },
     *   {
     *     $count: "view_count"
     *   }
     * ])
     *
     *
     */
    private fun _getMongoQueryViewCount(
        appIdList: List<String>,
        versions: List<String>,
        sortKey: String,
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
        val coll = _COLLFOR[sortKey] ?: IndicatorAllByView.COLLECTION_NAME_PT24H

        return coll to listOf(
            this._getPipeMatchQuery(appIdInObjIds, versions, sortKey),
            this._getPipeGroupByVhi(sortKey),
            D("\$count", "view_count"),
        )
    }

    /**
     * {
     *   "$match": {
     *     "ai": {
     *       "$in": [
     *         ObjectId("000000000000000000000004")
     *       ]
     *     },
     *     "av": {
     *       "$in": ["1.0.1"]
     *     },
     *     "vhi": {
     *       "$nin": [
     *         74180013,
     *         -1671032428
     *       ]
     *     }
     *     // for noresponse case
     *     "t": {$in:[1001, 1002, 1003, 1004]}
     *   }
     * },
     */
    private fun _getPipeMatchQuery(
        appIdList: List<ObjectId>,
        versionList: List<String>,
        sortKey: String
    ): D {
        val matchCod = D(
            Protocol.ai, D("\$in", appIdList)
        )
            .append(Protocol.vhi, D("\$nin", listOf(_VHI_SESSIONTSTART, _VHI_SESSIONTEND)))
        if (versionList.isNotEmpty()) {
            matchCod.append(Protocol.av, D("\$in", versionList))
        }
        if (sortKey === SORTKEY_SCREENVIEWACTIONNORESP) {
            matchCod.append(
                Protocol.t, D(
                    "\$in", listOf(
                        EventType.NOACT_TAP,
                        EventType.NOACT_DOUBLE_TAP,
                        EventType.NOACT_LONG_TAP,
                        EventType.NOACT_SWIPE
                    )
                )
            )
        }

        return D("\$match", matchCod)
    }

    /**
     * {
     *   "$group": {
     *     "_id": {
     *       "vhi": "$vhi"
     *     },
     *     "vhi": {$first: "$vhi"},
     *     "total": {
     *       "$sum": "$sco"
     *     }
     *   }
     * },
     */
    private fun _getPipeGroupByVhi(sortKey: String): D {

        return D(
            "\$group", D(
                "_id", D("vhi", "\$${Protocol.vhi}")
            )
                .append("vhi", D("\$first", "\$${Protocol.vhi}"))
                .append(_TMPFIELD_TOTALVAL, D("\$sum", "\$${_SORTFIELD[sortKey]}"))
        )
    }

    /**
     * @param sortOrder 1: ascending order / -1: descending order
     */

    private fun _getPipeSortBy(sortKey: String, sortOrder: Int): D {
        return D("\$sort", D(_TMPFIELD_TOTALVAL, sortOrder))
    }

    /**
     * {
     *   "$lookup": {
     *     "from": "view_list_pt24h",
     *     "let": {
     *       "vhi": "$vhi"
     *     },
     *     "pipeline": [
     *       {
     *         "$match": {
     *           "$expr": {
     *             "$and": [
     *               {"$eq": ["$vhi","$$vhi"]},
     *               {"$in":["$ai", [ObjectId("000000000000000000000004")]]},
     *               {"$in":["$av", ["1.0.1"]]}
     *             ]
     *           }
     *         }
     *       },
     *       {
     *           $project:{
     *               _id: 0,
     *           }
     *       }
     *     ],
     *     "as": "view"
     *   }
     * },
     */
    private fun _getPipeLookupViewInfo(
        appIdList: List<ObjectId>,
        versions: List<String>,
    ): D {
        val matchQuery = this._getLookupMatch(appIdList, versions)
        return D(
            "\$lookup",
            D("from", ViewList.COLLECTION_NAME_PT24H)
                .append("let", D("vhi", "\$${Protocol.vhi}"))
                .append(
                    "pipeline",
                    listOf(
                        matchQuery,
                        D("\$project", D("_id", 0))
                    )
                )
                .append("as", "view")
        )
    }

    /**
     * {
     *   "$match": {
     *     "$expr": {
     *       "$and": [
     *         {"$eq": ["$vhi","$$vhi"]},
     *         {"$in":["$ai", [ObjectId("000000000000000000000004")]]},
     *         {"$in":["$av", ["1.0.1"]]}
     *       ]
     *     }
     *   }
     * },
     */
    private fun _getLookupMatch(
        appIdList: List<ObjectId>,
        versions: List<String>,
    ): D {
        val matchCond = mutableListOf(
            D("\$eq", listOf("\$${Protocol.vhi}", "\$\$vhi")),
            D("\$in", listOf("\$${Protocol.ai}", appIdList)),
        )
        if (versions.isNotEmpty()) {
            matchCond += D("\$in", listOf("\$${Protocol.av}", versions))
        }
        return D(
            "\$match", D(
                "\$expr", D(
                    "\$and", matchCond
                )
            )
        )

    }
}