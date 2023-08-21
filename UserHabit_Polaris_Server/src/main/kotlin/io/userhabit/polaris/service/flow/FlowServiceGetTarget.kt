package io.userhabit.polaris.service.flow

import io.userhabit.batch.indicators.IndicatorByFlow
import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.service.AppService
import io.userhabit.polaris.service.FlowService
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P

class FlowServiceGetTarget {

    private val log = Loggers.getLogger(this.javaClass)


    fun getMongoResStreamTargetFlow(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        fromDate: String,
        toDate: String,
        fromViewId: String,
        toViewId: String,
        skip: Int,
        limit: Int
    ): Mono<List<D>> {

        return appIdStream.flatMap { ownedAppIdList ->

            val safeAppList: List<String> =
                if (appIdList.isEmpty()) {
                    ownedAppIdList
                } else {
                    appIdList.filter { appId ->
                        ownedAppIdList.contains(appId)
                    }
                }
            val (coll, query) = this.getMongoQueryOfTargetType(
                fromDate, toDate, safeAppList,
                versionList,
                fromViewId, toViewId, skip, limit
            )

            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(query))
                }
        }
    }

    /**
     * @return
     *
     * example
     *
     * {
     *     "status": 200,
     *     "message": "OK",
     *     "data": [
     *         {
     *             "depth": 2,
     *             "depth_count": 103,
     *             "flow_detail": [
     *                 {
     *                     "view_hash_id": -324351955,
     *                     "view_info": {
     *                         "_id": {
     *                             "timestamp": 1645522377,
     *                             "counter": 2489089,
     *                             "randomValue1": 14821649,
     *                             "randomValue2": -24953
     *                         },
     *                         "ai": {
     *                             "timestamp": 0,
     *                             "counter": 4,
     *                             "randomValue1": 0,
     *                             "randomValue2": 0
     *                         },
     *                         "av": "1.0.1",
     *                         "vhi": -324351955,
     *                         "vi": "(BrunchDetailActivity).(wv)brunch_detail_webview\"http://userhabit.io/\"[O1]",
     *                         "app_version": [
     *                             1,
     *                             0,
     *                             1
     *                         ]
     *                     }
     *                 },
     *                 {
     *                     "view_hash_id": -1671032428,
     *                     "view_info": {
     *                         ...
     *                     }
     *                 }
     *             ]
     *         }
     *     ],
     *     "field_list": []
     * }
     */
    fun getMongoResStreamTargetFlowsOfDepth(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        fromDate: String,
        toDate: String,
        fromViewId: String,
        toViewId: String,
        depth: Int,
    ): Mono<List<D>> {

        return appIdStream.flatMap { ownedAppIdList ->

            val safeAppList: List<String> =
                if (appIdList.isEmpty()) {
                    ownedAppIdList
                } else {
                    appIdList.filter { appId ->
                        ownedAppIdList.contains(appId)
                    }
                }
            val (coll, query) = this.getMongoQueryOfTargetTypeFlowsOfDepth(
                fromDate, toDate, safeAppList,
                versionList,
                fromViewId, toViewId, depth
            )
            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError { log.info(Util.toPretty(query)) }
        }
    }

    /**
     * @see https://github.com/userhabit/uh-issues/issues/391#issuecomment-1047311558
     *
     * return query example:
     *
     * ```
     * db.indicator_by_flow_pt24h.aggregate(
     * [
     *   {
     *     "$match": {
     *       "stz": {
     *         "$gte": ISODate('2022-02-08'),
     *         "$lte": ISODate('2022-02-22')
     *       },
     *       "ai": {
     *         "$in": [
     *           ObjectId("000000000000000000000004")
     *         ]
     *       },
     *       "flow": {
     *         "$regex": "^.*,(-324351955(,?.*,?)),-904521440"
     *       }
     *     }
     *   },
     *   {
     *     "$addFields": {
     *       "matchedFlow": {
     *         "$regexFind": {
     *           "input": "$flow",
     *           "regex": "^.*,(-324351955(,?.*,?)),-904521440"
     *         }
     *       }
     *     }
     *   },
     *   {
     *     "$set": {
     *       "matchedFlow": {
     *         "$first": "$matchedFlow.captures"
     *       }
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": "$matchedFlow",
     *       "count": {
     *         "$sum": "$fco"
     *       }
     *     }
     *   },
     *   {
     *     "$project": {
     *       "flow_detail": {
     *         "$split": [
     *           "$_id",
     *           ","
     *         ]
     *       },
     *       "count": 1
     *     }
     *   },
     *   {
     *     "$project": {
     *       "count": 1,
     *       "flow_detail": {
     *         "$map": {
     *           "input": "$flow_detail",
     *           "as": "array",
     *           "in": {
     *             "$toLong": "$$array"
     *           }
     *         }
     *       },
     *       "depth": { "$size": "$flow_detail" }
     *     }
     *   },
     *   {
     *     "$lookup": {
     *       "from": "view_list_pt24h",
     *       "let": {
     *         "vhi": "$flow_detail"  // $$vhi
     *       },
     *       "pipeline": [
     *         {
     *           "$match": {
     *             "$expr": {
     *               "$and": [
     *                 {
     *                  // check if $vhi in $$vhi
     *                   "$in": [
     *                     "$vhi",
     *                     "$$vhi"
     *                   ]
     *                 },
     *                 {},
     *                 {
     *                   "$in" : [
     *                     "$ai",
     *                     [ObjectId("000000000000000000000004")]
     *                   ]
     *                 }
     *               ]
     *             }
     *           }
     *         },
     *         {
     *           "$addFields": {
     *             "app_version": {
     *               "$map": {
     *                 "input": {
     *                   "$split": [
     *                     "$av",
     *                     "."
     *                   ]
     *                 },
     *                 "as": "t",
     *                 "in": {
     *                   "$toInt": "$$t"
     *                 }
     *               }
     *             }
     *           }
     *         },
     *         {
     *           "$sort": {
     *             "app_version.0": -1,
     *             "app_version.1": -1,
     *             "app_version.2": -1,
     *             "app_version.3": -1
     *           }
     *         }
     *       ],
     *       "as": "view_info"
     *     }
     *   },
     *   {
     *     "$project": {
     *       "count": 1,
     *       "depth": 1,
     *       "flow_detail": {
     *         "$map": {
     *           "input": "$flow_detail",
     *           "as": "array",
     *           // output
     *           "in": {
     *             "view_hash_id": "$$array",
     *             // var index = $view_info.vhi.indexOf(view_hash_id)
     *             // "view_info": $view_info[index]
     *             "view_info": {
     *               "$arrayElemAt": [
     *                 "$view_info",
     *                 {
     *                   "$indexOfArray": [
     *                     "$view_info.vhi",
     *                     "$$array"
     *                   ]
     *                 }
     *               ]
     *             }
     *           }
     *         }
     *       }
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": {
     *         "depth": "$depth",
     *         "flow_detail": "$flow_detail"
     *       },
     *       "count": {
     *         "$sum": "$count"
     *       }
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": {
     *         "depth": "$_id.depth"
     *       },
     *       "flow": {
     *         "$push": {
     *           "flow_detail": "$_id.flow_detail",
     *           "count": "$count"
     *         }
     *       },
     *       "depth_count": {
     *         "$sum": "$count"
     *       }
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": 0,
     *       "data": {
     *         "$push": {
     *           "depth": "$_id.depth",
     *           "depth_count": "$depth_count",
     *           "flow": "$flow"
     *         }
     *       },
     *       "total_depth_count": {
     *         "$sum": "$depth_count"
     *       }
     *     }
     *   },
     *   {
     *     "$unwind": "$data"
     *   },
     *   {
     *     "$project": {
     *       "_id": 0,
     *       "depth": "$data.depth",
     *       "flow": "$data.flow",
     *       "depth_count": "$data.depth_count",
     *       "depth_ratio": {
     *         "$multiply": [
     *           {
     *             "$divide": [
     *               "$data.depth_count",
     *               "$total_depth_count"
     *             ]
     *           },
     *           100
     *         ]
     *       }
     *     }
     *   },
     *   {
     *     "$sort": {
     *       "depth": 1
     *     }
     *   },
     *   {
     *     "$skip": 0
     *   },
     *   {
     *     "$limit": 40
     *   }
     * ])
     * ```
     */
    fun getMongoQueryOfTargetType(
        fromDate: String,
        toDate: String,
        appIdList: List<String>,
        versionList: List<String>,
        fromViewId: String,
        toViewId: String,
        skip: Int,
        limit: Int
    ): Pair<String, List<D>> {
        /**
         * 목표사용흐름
         * 시작, 목표화면의 스크린 키를 전달 받아 시작부터 목표까지의 단계별 화면정보와 집계 결과를 반환
         */
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return IndicatorByFlow.COLLECTION_NAME_PT24H to listOf(
            this._getPipeMatchFlowStr(fromDate, toDate, appIdInObjIds, versionList, fromViewId, toViewId),
            this._getPipeFindMatchedFlowStr(fromViewId, toViewId),
            this._getPipeExtractMatchedFlowStr(),
            this._getPipe2GroupByFlowStr(),
            this._getPipe3FlowDetailStrToArray(),
            this._getPipe4FlowDetailToLongNSetDepth(),
            this._getPipe5AttachViewInfoCandiOfFlowDetail(appIdInObjIds, versionList),
            this._getPipe6AddViewInfoToFlowDetail(),
            this._getPipe7GroupByDepthFlowDetail(),
            D(
                "\$group", D()
                    .append(
                        "_id", D()
                            .append("depth", "\$_id.depth")
                    )
                    .append(
                        "flow", D(
                            "\$push", D()
                                .append("flow_detail", "\$_id.flow_detail")
                                .append("count", "\$count")
                        )
                    )
                    .append("depth_count", D("\$sum", "\$count"))
            ),
            D(
                "\$group", D()
                    .append("_id", 0)
                    .append(
                        "data", D(
                            "\$push", D()
                                .append("depth", "\$_id.depth")
                                .append("depth_count", "\$depth_count")
                                .append("flow", "\$flow")
                        )
                    )
                    .append("total_depth_count", D("\$sum", "\$depth_count"))
            ),
            D("\$unwind", "\$data"),
            D(
                "\$project", D()
                    .append("_id", 0)
                    .append("depth", "\$data.depth")
                    .append("flow", "\$data.flow")
                    .append("depth_count", "\$data.depth_count")
                    .append(
                        "depth_ratio",
                        D("\$multiply", listOf(D("\$divide", listOf("\$data.depth_count", "\$total_depth_count")), 100))
                    )
            ),

            D("\$sort", D("depth", 1)),

            D("\$skip", skip),
            D("\$limit", limit)
        )

    }

    /**
     * @see https://github.com/userhabit/uh-issues/issues/391#issuecomment-1047311558
     *
     * return query example:
     *
     * ```
     * db.indicator_by_flow_pt24h.aggregate(
     * [
     *   {
     *     "$match": {
     *       "stz": {
     *         "$gte": ISODate('2022-02-08'),
     *         "$lte": ISODate('2022-02-22')
     *       },
     *       "ai": {
     *         "$in": [
     *           ObjectId("000000000000000000000004")
     *         ]
     *       },
     *       "flow": {
     *         "$regex": "^.*,74180013,([^,]*?,){2}330633654"
     *       }
     *     }
     *   },
     *   ...
     */
    fun getMongoQueryOfTargetTypeFlowsOfDepth(
        fromDate: String,
        toDate: String,
        appIdList: List<String>,
        versionList: List<String>,
        fromViewId: String,
        toViewId: String,
        depth: Int
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
        val matchQuery = this._getMatchOfDepth(
            fromDate, toDate,
            appIdInObjIds,
            fromViewId, toViewId,
            depth
        )


        return IndicatorByFlow.COLLECTION_NAME_PT24H to listOf(
            matchQuery,
            this._getPipeFindMatchedFlowStrOfDepth(fromViewId, toViewId, depth),
            this._getPipeExtractMatchedFlowStr(),
            this._getPipe2GroupByFlowStr(),
            this._getPipe3FlowDetailStrToArray(),
            this._getPipe4FlowDetailToLongNSetDepth(),
            this._getPipe5AttachViewInfoCandiOfFlowDetail(appIdInObjIds, versionList),
            this._getPipe6AddViewInfoToFlowDetail(),
            this._getPipe7GroupByDepthFlowDetail(),
            this._getPipeFormattingResultOfDepth(),
        )
    }


    private fun _getPipeMatchFlowStr(
        fromDate: String, toDate: String,
        appIdInObjIds: List<ObjectId>, versionList: List<String>,
        fromViewId: String, toViewId: String,
    ): D {
        val matchQuery = D(
            P.stz,
            D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
        )
            .append(P.ai, D("\$in", appIdInObjIds))
            .append(
                "flow", D("\$regex", this._getRegEx(fromViewId, toViewId))
            )
        if (versionList.isNotEmpty()) {
            matchQuery.append(P.av, D("\$in", versionList))
        }
        return D("\$match", matchQuery)
    }

    /**
     *  { $addFields: { matchedFlow: { $regexFind: { input: "$flow", regex: /^.*,(-324351955(,?.*,?)),-904521440/ }  } } },
     *
     */
    private fun _getPipeFindMatchedFlowStr(fromViewId: String, toViewId: String): D {
        return D(
            "\$addFields",
            D(
                "matchedFlow",
                D("\$regexFind", D("input", "\$flow").append("regex", this._getRegEx(fromViewId, toViewId)))
            )
        )
    }

    /**
     *
     *  { $addFields: { matchedFlow: { $regexFind: { input: "$flow", regex: /^.*,(-324351955(,[^,]*?){1}),-904521440/ }  } } },
     *
     */
    private fun _getPipeFindMatchedFlowStrOfDepth(fromViewId: String, toViewId: String, depth: Int): D {
        return D(
            "\$addFields",
            D(
                "matchedFlow",
                D(
                    "\$regexFind",
                    D("input", "\$flow").append("regex", this._getRegExOfDepth(fromViewId, toViewId, depth))
                )
            )
        )
    }

    /**
     * { $set: { matchedFlow: {$first: "$matchedFlow.captures" }} },
     */
    private fun _getPipeExtractMatchedFlowStr(): D {
        return D("\$set", D("matchedFlow", D("\$first", "\$matchedFlow.captures")))

    }

    /* {
     *   "$group": {
     *     "_id": "$matchedFlow",
     *     "count": {
     *       "$sum": "$fco"
     *     }
     *   }
     * },
     */
    private fun _getPipe2GroupByFlowStr(): D {
        return D(
            "\$group", D()
                .append("_id", "\$matchedFlow")
                .append("count", D("\$sum", "\$${P.fco}"))
        )
    }

    /**
     * {
     *   "$project": {
     *     // flow string 을 나눠서 array 에 넣는다.
     *     "flow_detail": {
     *       "$split": [ "$_id", "," ]
     *     },
     *     "count": 1
     *   }
     * },
     */
    private fun _getPipe3FlowDetailStrToArray(): D {
        return D(
            "\$project", D()
                .append("flow_detail", D("\$split", listOf("\$_id", ",")))
                .append("count", 1)
        )
    }

    /**
     *
     * If the flow is the '"222222,333333"', flow_detail would be '[NumberLong(222222)]'
     * {
     *   "$project": {
     *     "count": 1,
     *     "flow_detail": {
     *       "$map": {
     *         "input": "$flow_detail",
     *         "as": "array",
     *         "in": {
     *           "$toLong": "$$array"
     *         }
     *       }
     *     },
     *     // depth = flow_detail.length
     *     "depth": { "$size": "$flow_detail" }
     *   }
     * },
     */
    private fun _getPipe4FlowDetailToLongNSetDepth(): D {
        return D(
            "\$project", D()
                .append("count", 1)
                .append(
                    "flow_detail", D(
                        "\$map", D()
                            .append("input", "\$flow_detail")
                            .append("as", "array")
                            .append("in", D("\$toLong", "\$\$array"))
                    )
                )
                .append("depth", D("\$size", "\$flow_detail"))
        )
    }

    /**
     * this pipe creates 'view_info'
     * 'view_info' has the information of the vhi which is in the document of the 'view_list_pt24h'
     *
     * {
     *   "$lookup": {
     *     "from": "view_list_pt24h",
     *     "let": {
     *       "vhi": "$flow_detail"  // $$vhi
     *     },
     *     "pipeline": [
     *       {
     *         "$match": {
     *           "$expr": {
     *             "$and": [
     *               {
     *                // check if $vhi in $$vhi
     *                 "$in": ["$vhi","$$vhi"]
     *               },
     *               {}, // version
     *               {
     *                 "$in": ["$ai",[ObjectId("00000000004")]]
     *               }
     *             ]
     *           }
     *         }
     *       },
     *       {
     *         "$addFields": {
     *           "app_version": {
     *             "$map": {
     *               "input": {
     *                 "$split": ["$av","."]
     *               },
     *               "as": "t",
     *               "in": {
     *                 "$toInt": "$$t"
     *               }
     *             }
     *           }
     *         }
     *       },
     *       {
     *         "$sort": {
     *           "app_version.0": -1,
     *           "app_version.1": -1,
     *           "app_version.2": -1,
     *           "app_version.3": -1
     *         }
     *       }
     *     ],
     *     "as": "view_info"
     *   }
     * },
     */
    private fun _getPipe5AttachViewInfoCandiOfFlowDetail(appIdInObjIds: List<ObjectId>, versionList: List<String>): D {
        val versionDoc = if (versionList.isNotEmpty()) D("\$in", listOf("\$${P.av}", versionList)) else D()

        return D(
            "\$lookup", D()
                .append("from", ViewList.COLLECTION_NAME_PT24H)
                .append("let", D(P.vhi, "\$flow_detail"))
                .append(
                    "pipeline",
                    listOf(
                        D(
                            "\$match",
                            D(
                                "\$expr",
                                D(
                                    "\$and",
                                    listOf(
                                        D("\$in", listOf("\$${P.vhi}", "\$\$${P.vhi}")),
                                        versionDoc,
                                        D("\$in", listOf("\$${P.ai}", appIdInObjIds))
                                    )
                                )
                            )
                        ),
                        D(
                            "\$addFields",
                            D(
                                P.avK,
                                D(
                                    "\$map",
                                    D(
                                        "input",
                                        D("\$split", listOf("\$${P.av}", "."))
                                    )
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
                    )
                )
                .append("as", "view_info")
        )
    }

    /**
     * {
     *   "$project": {
     *     "count": 1,
     *     "depth": 1,
     *     "flow_detail": {
     *       "$map": {
     *         "input": "$flow_detail",
     *         "as": "array",
     *         // output
     *         "in": {
     *           "view_hash_id": "$$array",
     *           // var index = $view_info.vhi.indexOf(view_hash_id)
     *           // "view_info": $view_info[index]
     *           "view_info": {
     *             "$arrayElemAt": [
     *               "$view_info",
     *               {
     *                 // indexOfArray : if there is no matched element, returns -1
     *                 "$indexOfArray": [
     *                   "$view_info.vhi",
     *                   "$$array"
     *                 ]
     *               }
     *             ]
     *           }
     *         }
     *       }
     *     }
     *   }
     * },
     */
    private fun _getPipe6AddViewInfoToFlowDetail(): D {
        return D(
            "\$project", D()
                .append("count", 1)
                .append("depth", 1)
                .append(
                    "flow_detail", D(
                        "\$map", D()
                            .append("input", "\$flow_detail")
                            .append("as", "array")
                            .append(
                                "in", D()
                                    .append(P.vhiK, "\$\$array")
                                    .append(
                                        "view_info",
                                        D(
                                            // TODO: fix this query, because 'arrayElemAt' returns last item when the index is -1
                                            "\$arrayElemAt",
                                            listOf(
                                                "\$view_info",
                                                D("\$indexOfArray", listOf("\$view_info.${P.vhi}", "\$\$array"))
                                            )
                                        )
                                    )
                            )
                    )
                )
        )
    }

    /**
     *
     * {
     *   "$group": {
     *     "_id": {
     *       "depth": "$depth",
     *       "flow_detail": "$flow_detail"
     *     },
     *     "count": {
     *       "$sum": "$count"
     *     }
     *   }
     * },
     */
    private fun _getPipe7GroupByDepthFlowDetail(): D {
        return D(
            "\$group", D()
                .append(
                    "_id", D()
                        .append("depth", "\$depth")
                        .append("flow_detail", "\$flow_detail")
                )
                .append("count", D("\$sum", "\$count"))
        )
    }

    /**
     * {
     *   "$project": {
     *     "_id": 0,
     *     "depth": "$_id.depth",
     *     "flow_detail": "$_id.flow_detail",
     *     "depth_count": "$count",
     *   }
     * },
     */
    private fun _getPipeFormattingResultOfDepth(): D {
        return D(
            "\$project",
            D("_id", 0)
                .append("depth", "\$_id.depth")
                .append("flow_detail", "\$_id.flow_detail")
                .append("depth_count", "\$count")
        )
    }

    private fun _getMatchOfDepth(
        fromDate: String,
        toDate: String,
        appIdInObjIds: List<ObjectId>,
        fromViewId: String,
        toViewId: String,
        depth: Int
    ): D {

        val matchQuery = D(
            "\$match", D()
                .append(
                    P.stz, D("\$gte", Date.from(Instant.parse(fromDate)))
                        .append("\$lte", Date.from(Instant.parse(toDate)))
                )
                .append(P.ai, D("\$in", appIdInObjIds))
                .append(
                    "flow", D("\$regex", this._getRegExOfDepth(fromViewId, toViewId, depth))
                )
        )

        return matchQuery
    }

    /**
     * @return
     *   "^.*,(-324351955),$"
     *   "^.*,(-324351955(,?.*,?)),-904521440"
     */
    private fun _getRegEx(
        fromViewId: String,
        toViewId: String,
    ): String {
        if (fromViewId == toViewId) {
            return ",(${fromViewId})," // is it possible? end without SESSION_END?
        } else {
            return "^.*,(${fromViewId}(,?.*,?)),${toViewId}"
        }
    }

    /**
     * @return
     *   "^.*,(-324351955),$"
     *   "^.*,(-324351955(,[^,]*?){1}),-904521440"
     */
    private fun _getRegExOfDepth(
        fromViewId: String,
        toViewId: String,
        depth: Int
    ): String {
        if (fromViewId == toViewId) {
            return "^.*,(${fromViewId}),$" // is it possible? end without SESSION_END?
        } else {
            return "^.*,(${fromViewId}(,[^,]*?){${depth - 1}}),${toViewId}"
        }
    }
}