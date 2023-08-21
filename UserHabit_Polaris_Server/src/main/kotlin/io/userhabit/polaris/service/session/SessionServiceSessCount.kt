package io.userhabit.polaris.service.session

import io.userhabit.batch.indicators.IndicatorAllByView
import io.userhabit.batch.indicators.IndicatorDeviceByOsDevicename
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.Protocol
import org.bson.types.ObjectId
import org.bson.Document as D
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*


class SessionServiceSessCount {

    private val log = Loggers.getLogger(this.javaClass)
    private val _FIELDNAME_SESSIONCOUNT = "session_count"
    private val dateFormat = mapOf<String, String>(
        "month" to "%Y-%m-01T00:00:00.000Z",
        "day" to "%Y-%m-%dT00:00:00.000Z",
        "hour" to "%Y-%m-%dT%H:00:00.000Z",
    )

    fun getMongoResStreamSessCountOfEachScreen(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
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
            val (coll, query) = this._getMongoQuerySessCountOfEachView(safeAppList, versionList)

            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(query))
                }
        }
    }

    fun getMongoResStreamSessCountOfDeviceId(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        fromto: List<Date>,
        interval: String
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
            val (coll, query) = this._getMongoQuerySessCountOfDeviceId(
                safeAppList,
                versionList,
                fromto,
                interval
            )

            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(query))
                }
        }
    }

    /**
     * ```
     * db.indicator_all_by_view_pt24h.aggregate([
     *  {
     *      "$match": {
     *          "ai": {
     *              "$in": [ObjectId("000000000000000000000004")]
     *          },
     *          "av": {
     *              "$in": ["1.0.1"]
     *          }
     *      }
     *  },
     *  {
     *      $group: {
     *          _id: "$vhi",
     *          "session_count": {
     *              "$sum": "$sco"
     *          }
     *      },
     *
     *  },
     *  // formatting
     *  {
     *      $project: {
     *          _id: 0,
     *          vhi: "$_id",
     *          session_count: "$session_count",
     *      }
     *  }
     * ]
     * ```
     */
    private fun _getMongoQuerySessCountOfEachView(
        appIdList: List<String>,
        versions: List<String>
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return IndicatorAllByView.COLLECTION_NAME_PT24H to listOf(
            this._getPipeMatchQuery(appIdInObjIds, versions),
            this._getPipeGroupByVhi(),
            this._getPipeFormatting()
        )
    }

    /**
     * db.indicator_device_by_os_devicename_pt24h.aggregate(
     *   [
     *     {
     *       "$match": {
     *         "ai": {
     *           "$in": [
     *             ObjectId("000000000000000000000004")
     *           ]
     *         },
     *         "stz": {
     *           "$gte": ISODate("2022-03-08"),
     *           "$lt": ISODate("2022-03-22"),
     *         }
     *       }
     *     },
     *     {
     *       $group: {
     *         "_id": {
     *           "stz": {
     *             "$dateToString": {
     *               "format": "%Y-%m-01T00:00:00.000Z",
     *               "date": "$stz"
     *             }
     *           }
     *         },
     *         "dco": {
     *           "$sum": "$dco"
     *         }
     *       }
     *     },
     *     {
     *       "$project": {
     *         "_id": 0,
     *         "session_time": "$_id.stz",
     *         "device_count": "$dco"
     *       }
     *     }
     *   ]
     * )
     */
    private fun _getMongoQuerySessCountOfDeviceId(
        appIdList: List<String>,
        versions: List<String>,
        fromto: List<Date>,
        interval: String,
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT24H to listOf(
            this._getPipeMatchQuery(
                appIdInObjIds,
                versions,
                fromto
            ),
            this._getPipeGroupByTime(interval),
            this._getPipeFormattingOfSessCountOfDeviceId()
        )
    }

    /**
     *  {
     *      "$match": {
     *          "ai": {
     *              "$in": [ObjectId("000000000000000000000004")]
     *          },
     *          "av": {
     *              "$in": ["1.0.1"]
     *          },
     *          "stz: {
     *              "$gte": ISODate("2022-03-01"),
     *              "$lt": ISODate("2022-03-03")
     *          }
     *      }
     *  },
     */
    private fun _getPipeMatchQuery(
        appIdList: List<ObjectId>,
        versionList: List<String>,
        fromto: List<Date> = listOf(),
    ): D {
        val matchCod = D(
            Protocol.ai, D("\$in", appIdList)
        )
        if (versionList.isNotEmpty()) {
            matchCod.append(Protocol.av, D("\$in", versionList))
        }

        if (fromto.isNotEmpty()) {
            matchCod.append(
                Protocol.stz,
                D("\$gte", fromto[0])
                    .append("\$lt", fromto[1])
            )
        }

        return D("\$match", matchCod)
    }


    /**
     *  {
     *      $group: {
     *          _id: "$vhi",
     *          "session_count": {
     *              "$sum": "$sco"
     *          }
     *      },
     *
     *  },
     */
    private fun _getPipeGroupByVhi(): D {
        return D(
            "\$group",
            D("_id", "\$${Protocol.vhi}").append(this._FIELDNAME_SESSIONCOUNT, D("\$sum", "\$${Protocol.sco}"))
        )
    }

    /**
     *  {
     *   "$group": {
     *     "_id": {
     *       "stz": {
     *         "$dateToString": {
     *           "format": "%Y-%m-01T00:00:00.000Z",
     *           "date": "$stz"
     *         }
     *       }
     *     },
     *     "dco": {
     *       "$sum": "$dco"
     *     }
     *   }
     * },
     */
    private fun _getPipeGroupByTime(interval: String): D {

        return D(
            "\$group",
            D(
                "_id", D(
                    Protocol.stz,
                    D(
                        "\$dateToString",
                        D("format", this.dateFormat[interval])
                            .append("date", "\$${Protocol.stz}")
                    )

                )
            )
                .append(Protocol.dco, D("\$sum", "\$${Protocol.dco}"))
        )
    }

    /**
     *  // formatting
     *  {
     *      $project: {
     *          _id: 0,
     *          vhi: "$_id",
     *          session_count: "$session_count",
     *      }
     *  }
     */
    private fun _getPipeFormatting(): D {
        return D(
            "\$project",
            D("_id", 0).append("vhi", "\$_id").append("session_count", "\$${this._FIELDNAME_SESSIONCOUNT}")
        )
    }

    /**
     *  // formatting
     *  {
     *      $project: {
     *          _id: 0,
     *          session_time: "$_id.stz",
     *          device_count: "$dco",
     *      }
     *  }
     */
    private fun _getPipeFormattingOfSessCountOfDeviceId(): D {
        return D(
            "\$project",
            D("_id", 0)
                .append(Protocol.stzK, "\$_id.${Protocol.stz}")
                .append(Protocol.dcoK, "\$${Protocol.dco}")
        )
    }

}