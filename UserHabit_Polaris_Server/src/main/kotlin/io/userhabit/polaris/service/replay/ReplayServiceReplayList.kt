package io.userhabit.polaris.service.replay

import io.userhabit.batch.indicators.IndicatorAllByView
import io.userhabit.batch.indicators.IndicatorDeviceByOsDevicename
import io.userhabit.common.Level
import io.userhabit.common.MongodbUtil
import io.userhabit.common.PolarisException
import io.userhabit.common.Util
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.EventType
import io.userhabit.polaris.Protocol
import io.userhabit.polaris.service.SessionService
import org.bson.BSONException
import org.bson.types.ObjectId
import org.bson.Document as D
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import kotlin.math.max


class ReplayServiceReplayList {

    private val log = Loggers.getLogger(this.javaClass)

    /**
     * @param from "YYYY-MM-DDTHH:mm:ss.000Z"
     * @param sortOrder 1|-1
     *   - 1: ascending order
     *   - -1 : descending order
     */
    fun getMongoResStreamReplayList(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        from: String,
        to: String,
        deviceId: String,
        searchExpr: String,
        sortField: String,
        sortOrder: Int,
        skip: Int,
        limit: Int,
    ): Mono<List<D>> {
        return appIdStream.flatMap { ownedAppIdList ->

            val safeAppList: List<String> = this._getSafeAppList(ownedAppIdList, appIdList)

            val (coll, query) = this._getMongoQueryReplayList(
                safeAppList,
                versionList,
                from,
                to,
                deviceId,
                searchExpr,
                sortField,
                sortOrder,
                skip,
                limit
            )

            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(query))
                }
        }
    }

    /**
     * @param from "YYYY-MM-DDTHH:mm:ss.000Z"
     */
    fun getMongoResStreamReplayListTotalCount(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        from: String,
        to: String,
        deviceId: String,
        searchExpr: String
    ): Mono<List<D>> {
        return appIdStream.flatMap { ownedAppIdList ->

            val safeAppList: List<String> = this._getSafeAppList(ownedAppIdList, appIdList)

            val (coll, query) = this._getMongoQueryReplayListTotalCount(safeAppList, versionList, from, to, deviceId, searchExpr)

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

    private fun _getMongoQueryReplayListTotalCount(
        appIdList: List<String>,
        versions: List<String>,
        from: String,
        to: String,
        deviceId: String,
        searchExpr: String,
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return SessionService.COLLECTION_NAME to listOf(
            this._get1stMatchQuery(appIdInObjIds, from, to, deviceId, searchExpr),
            D("\$count", "totalDocs")
        )
    }

    /**
     * ```
     * db.session.aggregate(
     * [
     *   {
     *     "$match": {
     *       "st": {
     *         "$gte": ISODate("2022-04-30"),
     *         "$lt": ISODate("2022-05-07"),
     *       },
     *       "ai": {
     *         "$in": [
     *           ObjectId("000000000000000000000004")
     *         ]
     *       },
     *
     *     }
     *   },
     *   {
     *     "$sort": {
     *       "dl": 1, "st": 1
     *     }
     *   },
     *   {
     *     "$skip": 0
     *   },
     *   {
     *     "$limit": 10
     *   },
     *   {
     *     "$lookup": {
     *       "from": "event",
     *       "localField": "_id",
     *       "foreignField": "_id.si",
     *       "as": "event"
     *     }
     *   },
     *   {
     *     "$project": {
     *       "_id": 1,
     *       "st": 1,
     *       "se": 1,
     *       "di": 1,
     *       "dn": 1,
     *       "do": 1,
     *       "dov": 1,
     *       "av": 1,
     *       "sn": 1,
     *       "ise": 1,
     *       "dl": 1,
     *       "dz": 1,
     *       "dw": 1,
     *       "dh": 1,
     *       "startEndView": {
     *         "$filter": {
     *           "input": "$event",
     *           "as": "event",
     *           "cond": {
     *             "$in": [
     *               "$$event.t",
     *               [
     *                 4101,
     *                 4100,
     *                 8200
     *               ]
     *             ]
     *           }
     *         }
     *       },
     *       "startView": {
     *         "$filter": {
     *           "input": "$event",
     *           "as": "event",
     *           "cond": {
     *             "$eq": [
     *               "$$event.t",
     *               4101
     *             ]
     *           }
     *         }
     *       },
     *       "actEvent": {
     *         "$filter": {
     *           "input": "$event",
     *           "as": "event",
     *           "cond": {
     *             "$in": [
     *               "$$event.t",
     *               [
     *                 1001,
     *                 1101,
     *                 1103,
     *                 1102,
     *                 1104,
     *                 1606
     *               ]
     *             ]
     *           }
     *         }
     *       },
     *       "crashEvent": {
     *         "$filter": {
     *           "input": "$event",
     *           "as": "event",
     *           "cond": {
     *             "$eq": [
     *               "$$event.t",
     *               3400
     *             ]
     *           }
     *         }
     *       },
     *       "backgroundEvent": {
     *         "$filter": {
     *           "input": "$event",
     *           "as": "event",
     *           "cond": {
     *             "$eq": [
     *               "$$event.t",
     *               8200
     *             ]
     *           }
     *         }
     *       }
     *     }
     *   },
     *   {
     *     "$project": {
     *       "session_time": {
     *         "$dateToString": {
     *           "date": "$_id.st",
     *           "format": "%Y-%m-%dT%H:%M:%S.%LZ"
     *         }
     *       },
     *       "session_exp": "$se",
     *       "device_id": "$di",
     *       "device_name": "$dn",
     *       "device_os": "$do",
     *       "device_os_ver": "$dov",
     *       "app_version": "$av",
     *       "session_net": "$sn",
     *       "ise": "$ise",
     *       "session_id": "$_id",
     *       "device_lang": "$dl",
     *       "device_zone": "$dz",
     *       "device_wh": {
     *         "$concat": [
     *           {
     *             "$toString": "$dw"
     *           },
     *           "x",
     *           {
     *             "$toString": "$dh"
     *           }
     *         ]
     *       },
     *       "event_count": {
     *         "$size": "$actEvent"
     *       },
     *       "vis": {
     *         "$setUnion": {
     *           "$reduce": {
     *             "input": "$startView.vi",
     *             "initialValue": [],
     *             "in": {
     *               "$concatArrays": [
     *                 "$$value",
     *                 [
     *                   "$$this"
     *                 ]
     *               ]
     *             }
     *           }
     *         }
     *       },
     *       "crash_id": {
     *         "$setUnion": {
     *           "$reduce": {
     *             "input": "$crashEvent.ci",
     *             "initialValue": [],
     *             "in": {
     *               "$concatArrays": [
     *                 "$$value",
     *                 [
     *                   "$$this"
     *                 ]
     *               ]
     *             }
     *           }
     *         }
     *       },
     *       "dwell_time": {
     *         "$max": "$startEndView._id.ts"
     *       }
     *     }
     *   },
     *   {
     *       $addFields: {
     *         unique_view_count: { $size: "$vis" } ,
     *       }
     *   }
     * ]
     * ```
     */
    private fun _getMongoQueryReplayList(
        appIdList: List<String>,
        versions: List<String>,
        from: String,
        to: String,
        deviceId: String,
        searchExpr: String,
        sortField: String,
        sortOrder: Int,
        skip: Int,
        limit: Int,
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return SessionService.COLLECTION_NAME to listOf(
            this._get1stMatchQuery(appIdInObjIds, from, to, deviceId, searchExpr),
            this._get1stSort(sortField, sortOrder),
            // TODO: consider, $facet
            D("\$skip", skip),
            D("\$limit", limit),
            this._getLookupEventOfSessions(),
            this._getProjection(),
            this._getProjection2(),
            D("\$addFields", D("unique_view_count", D("\$size", "\$vis")))

//            D("\$unwind", "\$startView"),
//            D("\$unwind", D("path", "\$crashEvent").append("preserveNullAndEmptyArrays", true)),
//            this._getGrouping(),
//            this._getReplaceWith(),
//
//            D(
//                "\$facet", D(
//                    "pagenatedResults", listOf(
//
//                    )
//                ).append(
//                    "totalCount", listOf(
//                        D("\$count", "totalDocs")
//                    )
//                )
//            )

        )
    }

    fun _get1stMatchQuery(
        appIdInObjIds: List<ObjectId>, from: String, to: String, deviceId: String, searchExpr: String
    ): D {

        val matchQuery = D("st",
            D("\$gte", Date.from(Instant.parse(from)))
                .append("\$lt", Date.from(Instant.parse(to)))
        )
            .append("ai", D("\$in", appIdInObjIds))

        // device_id filter
        if (deviceId.isNotEmpty()) {
            matchQuery.append(Protocol.di, deviceId)
        }

        // other filters
        val filterNotForHere = listOf(Protocol.dtK, Protocol.ecoK, Protocol.vcoK, Protocol.uvcoK, Protocol.ciK)
        val searchDoc = this._parseFilter(searchExpr)
        for (item in searchDoc) {
            if (filterNotForHere.indexOf(item.key) != -1) {
                // if the key is not applicable here
                continue
            }
            matchQuery.append(Protocol.getAbbreviation(item.key), item.value)
        }

        return D("\$match", matchQuery)


        // old code for filter
//            if (!Level.hasPermission(req.getLevel(), Level.SYSTEM)) matchDoc.append(
//                Protocol.ai,
//                D("\$in", QueryPeasant.convertToObjectIdList(appIdList))
//            )

    }

    fun _parseFilter(searchExpr: String): D {
        val searchDoc = try {
            D.parse(searchExpr)
        } catch (e: BSONException) {
            throw PolarisException.status400BadRequest(listOf(mapOf("search_expr" to e.message)))
        }
        return searchDoc
    }

    fun _getLookupEventOfSessions(): D {
        return D(
            "\$lookup", D("from", "event")
            .append("localField", "_id")
            .append("foreignField", "_id.${Protocol.si}")
            .append("as", "event")
        )
    }

    /**
     * {
     *   "$project": {
     *     "_id": 1,
     *     "st": 1,
     *     "se": 1,
     *     "di": 1,
     *     "dn": 1,
     *     "do": 1,
     *     "dov": 1,
     *     "av": 1,
     *     "sn": 1,
     *     "ise": 1,
     *     "dl": 1,
     *     "dz": 1,
     *     "dw": 1,
     *     "dh": 1,
     *     "startEndView": {
     *       "$filter": {
     *         "input": "$event",
     *         "as": "event",
     *         "cond": {
     *           "$in": [
     *             "$$event.t",
     *             [
     *               4101,
     *               4100
     *             ]
     *           ]
     *         }
     *       }
     *     },
     *     "startView": {
     *       "$filter": {
     *         "input": "$event",
     *         "as": "event",
     *         "cond": {
     *           "$eq": [
     *             "$$event.t",
     *             4101
     *           ]
     *         }
     *       }
     *     },
     *     "actEvent": {
     *       "$filter": {
     *         "input": "$event",
     *         "as": "event",
     *         "cond": {
     *           "$in": [
     *             "$$event.t",
     *             [
     *               1001,
     *               1101,
     *               1103,
     *               1102,
     *               1104,
     *               1606
     *             ]
     *           ]
     *         }
     *       }
     *     },
     *     "crashEvent": {
     *       "$filter": {
     *         "input": "$event",
     *         "as": "event",
     *         "cond": {
     *           "$eq": [
     *             "$$event.t",
     *             3400
     *           ]
     *         }
     *       }
     *     },
     *     "backgroundEvent": {
     *       "$filter": {
     *         "input": "$event",
     *         "as": "event",
     *         "cond": {
     *           "$eq": [
     *             "$$event.t",
     *             8200
     *           ]
     *         }
     *       }
     *     }
     *   }
     * }
     */
    fun _getProjection(): D {
        return D(
            mapOf(
                "\$project" to mapOf(
                    "_id" to 1,
                    Protocol.st to 1,
                    Protocol.se to 1,
                    Protocol.di to 1,
                    Protocol.dn to 1,
                    Protocol.`do` to 1,
                    Protocol.dov to 1,
                    Protocol.av to 1,
                    Protocol.sn to 1,
                    "i${Protocol.se}" to 1,
                    Protocol.dl to 1,
                    Protocol.dz to 1,
                    Protocol.dw to 1,
                    Protocol.dh to 1,
                    "startEndView" to mapOf(
                        "\$filter" to mapOf(
                            "input" to "\$event",
                            "as" to "event",
                            "cond" to mapOf(
                                "\$in" to listOf(
                                    "\$\$event.t",
                                    listOf(EventType.VIEW_START, EventType.VIEW_END, EventType.APP_BACKGROUND)
                                )
                            )
                        )
                    ),
                    "startView" to mapOf(
                        "\$filter" to mapOf(
                            "input" to "\$event",
                            "as" to "event",
                            "cond" to mapOf("\$eq" to listOf("\$\$event.${Protocol.t}", EventType.VIEW_START))
                        )
                    ),
                    "actEvent" to mapOf(
                        "\$filter" to mapOf(
                            "input" to "\$event",
                            "as" to "event",
                            "cond" to mapOf(
                                "\$in" to listOf(
                                    "\$\$event.${Protocol.t}",
                                    listOf(
                                        EventType.NOACT_TAP,
                                        EventType.REACT_TAP,
                                        EventType.REACT_LONG_TAP,
                                        EventType.REACT_DOUBLE_TAP,
                                        EventType.REACT_SWIPE,
                                        EventType.SCROLL_CHANGE
                                    )
                                )
                            )
                        )
                    ),
                    "crashEvent" to mapOf(
                        "\$filter" to mapOf(
                            "input" to "\$event",
                            "as" to "event",
                            "cond" to mapOf("\$eq" to listOf("\$\$event.${Protocol.t}", EventType.CRASH))
                        )
                    ),
                    "backgroundEvent" to mapOf(
                        "\$filter" to mapOf(
                            "input" to "\$event",
                            "as" to "event",
                            "cond" to mapOf("\$eq" to listOf("\$\$event.${Protocol.t}", EventType.APP_BACKGROUND))
                        )
                    ),
                    // Add odk odv
//						"optionDataEvent" to mapOf("\$filter" to mapOf(
//							"input" to "\$event",
//							"as" to "event",
//							"cond" to mapOf("\$eq" to listOf("\$\$event.${P.t}", ET.USER_CONTENT))
//						))
                )
            )
        )
    }

    /**
     * {
     *   "$project": {
     *     session_time: {
     *       "$dateToString": {
     *         "date": "$_id.st",
     *         "format": "%Y-%m-%dT%H:%M:%S.%LZ"
     *       }
     *     },
     *     "session_exp": "$se",
     *     "device_id": "$di",
     *     "device_name": "$dn",
     *     "device_os": "$do",
     *     "device_os_ver": "$dov",
     *     "app_version": "$av",
     *     "session_net": "$sn",
     *     "ise": "$ise",
     *     "session_id": "$_id",
     *     "device_lang": "$dl",
     *     "device_zone": "$dz",
     *     "device_wh": {
     *            "$concat": [
     *              {
     *                "$toString": "$dw"
     *              },
     *              "x",
     *              {
     *                "$toString": "$dh"
     *              }
     *            ]
     *          },
     *
     *     "event_count": {"$size": "$actEvent"},
     *     "vis_tmp": {
     *       "$map": {
     *         "input": "$startView",
     *         "as": "sv",
     *         "in": "$$sv.vi"
     *       }
     *     },
     *     vis: {
     *       $setUnion: {
     *         $reduce: {
     *           "input": "$startView.vi",
     *           initialValue: [],
     *           in: { $concatArrays: ["$$value", ["$$this"]] },
     *
     *         },
     *       },
     *     },
     *
     *     "dwell_time": { $max: "$startEndView._id.ts" }
     *
     *   }
     * },
     */
    fun _getProjection2(): D {
        val prjQuery = D(
            "session_time",
            D("\$dateToString",
                D("date", "\$st")
                    .append("format", "%Y-%m-%dT%H:%M:%S.%LZ")
            )
        ).append("session_exp", "\$se")
            .append("device_id", "\$di")
            .append("device_name", "\$dn")
            .append("device_os", "\$do")
            .append("device_os_ver", "\$dov")
            .append("app_version", "\$av")
            .append("session_net", "\$sn")
            .append("ise", "\$ise")
            .append("session_id", "\$_id")
            .append("device_lang", "\$dl")
            .append("device_zone", "\$dz")
            .append("device_wh", D("\$concat", listOf(
                D("\$toString", "\$dw"),
                "x",
                D("\$toString", "\$dh"),
            )))
            .append("event_count", D("\$size", "\$actEvent"))
            .append("view_count", D("\$size", "\$startView"))
            .append("dwell_time", D("\$max", "\$startEndView._id.ts"))
            .append("vis", D("\$setUnion",
                D("\$reduce", D(
                    "input", "\$startView.vi"
                )
                    .append("initialValue", listOf<Unit>())
                    .append("in", D(
                        "\$concatArrays", listOf("\$\$value", listOf("\$\$this"))
                    ))
                )
            ))
            .append("crash_id", D("\$setUnion",
                D("\$reduce", D(
                    "input", "\$crashEvent.ci"
                )
                    .append("initialValue", listOf<Unit>())
                    .append("in", D(
                        "\$concatArrays", listOf("\$\$value", listOf("\$\$this"))
                    ))
                )
            ))


        return D("\$project", prjQuery)
    }

    /**
     * {
     *   "$sort": {
     *     "dl": 1,
     *     // for sort consistency
     *     // https://www.mongodb.com/docs/manual/reference/operator/aggregation/sort/#sort-consistency
     *     "st": -1,
     *   }
     * },
     */
    fun _get1stSort(sortField: String, sortOrder: Int): D {
        val sortKey = Protocol.getAbbreviation2(sortField)
        val sortQuery = D(sortKey, sortOrder)
        if (sortKey != Protocol.si) {
            sortQuery.append(Protocol.si, -1)
        }
        return D("\$sort", sortQuery)
    }

    /**
     * {
     *   "$group": {
     *     "_id": {
     *       "_id": "$_id",
     *       "st": "$st",
     *       "se": "$se",
     *       "di": "$di",
     *       "dn": "$dn",
     *       "do": "$do",
     *       "dov": "$dov",
     *       "av": "$av",
     *       "sn": "$sn",
     *       "ise": "$ise",
     *       "dl": "$dl",
     *       "dz": "$dz",
     *       "dw": "$dw",
     *       "dh": "$dh",
     *       "maxDt": {
     *         "_id": {
     *           "$first": "$startEndView._id.si"
     *         },
     *         "dt": {
     *           "$max": {
     *             "$concatArrays": [
     *               "$startEndView._id.ts",
     *               "$backgroundEvent._id.ts"
     *             ]
     *           }
     *         }
     *       },
     *       "ecoList": {
     *         "eco": {
     *           "$size": "$actEvent"
     *         }
     *       }
     *     },
     *     "vco": {
     *       "$push": "$startView.vi"
     *     },
     *     "uvco": {
     *       "$addToSet": "$startView.vi"
     *     },
     *     "ci": {
     *       "$addToSet": "$crashEvent.ci"
     *     }
     *   }
     * },
     */
    fun _getGrouping(): D {
        return D(
            mapOf(
                "\$group" to mapOf(
                    "_id" to mapOf(
                        "_id" to "\$_id",
                        Protocol.st to "\$st",
                        Protocol.se to "\$se",
                        Protocol.di to "\$di",
                        Protocol.dn to "\$dn",
                        Protocol.`do` to "\$${Protocol.`do`}",
                        Protocol.dov to "\$dov",
                        Protocol.av to "\$av",
                        Protocol.sn to "\$sn",
                        "i${Protocol.se}" to "\$i${Protocol.se}",
                        Protocol.dl to "\$dl",
                        Protocol.dz to "\$dz",
                        Protocol.dw to "\$dw",
                        Protocol.dh to "\$dh",
                        "maxDt" to mapOf(
                            "_id" to mapOf("\$first" to "\$startEndView._id.si"),
                            "dt" to mapOf(
                                "\$max" to mapOf(
                                    "\$concatArrays" to listOf(
                                        "\$startEndView._id.ts",
                                        "\$backgroundEvent._id.ts"
                                    )
                                )
                            ),
                        ),
                        "ecoList" to mapOf("eco" to mapOf("\$size" to "\$actEvent"))
                    ),
                    Protocol.vco to mapOf("\$push" to "\$startView.vi"),
                    Protocol.uvco to mapOf("\$addToSet" to "\$startView.vi"),
                    Protocol.ci to mapOf("\$addToSet" to "\$crashEvent.ci"),
                    // Todo Add: Not User optionDataEvent !
                )
            )
        )
    }

    /**
     * {
     *   "$replaceWith": {
     *     "$mergeObjects": [
     *       {
     *         "session_time": {
     *           "$dateToString": {
     *             "date": "$_id.st",
     *             "format": "%Y-%m-%dT%H:%M:%S.%LZ"
     *           }
     *         },
     *         "session_exp": "$_id.se",
     *         "dwell_time": "$_id.maxDt.dt",
     *         "view_count": {
     *           "$size": "$vco"
     *         },
     *         "unique_view_count": {
     *           "$size": "$uvco"
     *         },
     *         "event_count": {
     *           "$ifNull": [
     *             "$_id.ecoList.eco",
     *             0
     *           ]
     *         },
     *         "device_id": "$_id.di",
     *         "device_name": "$_id.dn",
     *         "device_os": "$_id.do",
     *         "device_os_ver": "$_id.dov",
     *         "app_version": "$_id.av",
     *         "session_net": "$_id.sn",
     *         "ise": "$_id.ise",
     *         "session_id": "$_id._id",
     *         "device_lang": "$_id.dl",
     *         "device_zone": "$_id.dz",
     *         "device_wh": {
     *           "$concat": [
     *             {
     *               "$toString": "$_id.dw"
     *             },
     *             "x",
     *             {
     *               "$toString": "$_id.dh"
     *             }
     *           ]
     *         },
     *         "crash_id": {
     *           "$ifNull": [
     *             "$ci",
     *             []
     *           ]
     *         }
     *       }
     *     ]
     *   }
     * },
     */
    fun _getReplaceWith(): D {
        return D(
            "\$replaceWith", D(
            D(
                "\$mergeObjects", listOf(
                D()
                    .append(
                        Protocol.stK,
                        D(
                            "\$dateToString",
                            D("date", "\$_id.${Protocol.st}").append("format", "%Y-%m-%dT%H:%M:%S.%LZ")
                        )
                    )
                    .append(Protocol.seK, "\$_id.${Protocol.se}")
//								.append(P.dtK, D("\$floor", D("\$divide", listOf("\$_id.maxDt.${P.dt}", 1000))))
                    .append(Protocol.dtK, "\$_id.maxDt.${Protocol.dt}")
                    .append(Protocol.vcoK, D("\$size", "\$${Protocol.vco}"))
                    .append(Protocol.uvcoK, D("\$size", "\$${Protocol.uvco}"))
                    .append(Protocol.ecoK, D("\$ifNull", listOf("\$_id.ecoList.${Protocol.eco}", 0)))
                    .append(Protocol.diK, "\$_id.${Protocol.di}")
                    .append(Protocol.dnK, "\$_id.${Protocol.dn}")
                    .append(Protocol.doK, "\$_id.${Protocol.`do`}")
                    .append(Protocol.dovK, "\$_id.${Protocol.dov}")
                    .append(Protocol.avK, "\$_id.${Protocol.av}")
                    .append(Protocol.snK, "\$_id.${Protocol.sn}")
                    .append("i${Protocol.se}", "\$_id.i${Protocol.se}") // TODO isc?
                    .append(Protocol.siK, "\$_id._id")
                    .append(Protocol.dlK, "\$_id.${Protocol.dl}")
                    .append(Protocol.dzK, "\$_id.${Protocol.dz}")
                    .append(
                        Protocol.dwhK, D(
                        "\$concat",
                        listOf(
                            D("\$toString", "\$_id.${Protocol.dw}"),
                            "x",
                            D("\$toString", "\$_id.${Protocol.dh}")
                        )
                    )
                    )
                    .append(Protocol.ciK, D("\$ifNull", listOf("\$${Protocol.ci}", emptyList<String>())))
            )
            )
        )
        )
    }
}