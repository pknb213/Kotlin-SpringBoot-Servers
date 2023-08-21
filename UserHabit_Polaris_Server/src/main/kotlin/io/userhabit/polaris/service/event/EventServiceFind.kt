package io.userhabit.polaris.service.event


import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import io.userhabit.common.utils.QueryPeasant

import io.userhabit.polaris.Protocol
import io.userhabit.polaris.service.EventService
import org.bson.types.ObjectId
import org.bson.Document as D
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers


class EventServiceFind {
    private val log = Loggers.getLogger(this.javaClass)


    fun getMongoResStreamEventsOfSession(
        ownedAppIdList: List<String>,
        appIdList: List<String>,
        sessionId: String,
        skip: Int,
        limit: Int
    ): Mono<List<D>> {

        val safeAppList: List<String> = this._getSafeAppList(ownedAppIdList, appIdList)

        val (coll, query) = this._getMongoQueryEventsOfSession(
            sessionId,
            safeAppList,
            skip,
            limit
        )

        return Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
            .collectList()
            .doOnError {
                log.info(Util.toPretty(query))
            }
    }

    fun getMongoResStreamEventsOfSession2(
        sessionId: String,
        limit: Int,
        sortCond: D?
    ): Mono<List<D>> {
        1
        val findQuery = this._getFindQueryEventOfSession(sessionId, limit, sortCond)

        return Flux.from(findQuery)
            .collectList()
            .doOnError {
                log.info(Util.toPretty(findQuery))
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
     * db.event.aggregate(
     *     [
     *         {
     *             $match: {
     *                 "_id.si": "ffce62fa592e028a26a1e5bf12550597a8e36792"
     *             }
     *         },
     *         {
     *             "$sort": {
     *                 "ts": -1
     *             }
     *         },
     *         {
     *             $skip: 0
     *         },
     *         {
     *             $limit: 10
     *         },
     *         {
     *             $lookup: {
     *                 from: "view_list_pt24h",
     *                 let: {
     *                     event_vhi: "$vhi"
     *                 },
     *                 pipeline: [
     *                     {
     *                         $match: {
     *                             $expr: {
     *                                 $and: [
     *                                     { $eq: ["$ai", ObjectId("000000000000000000000004")] },
     *                                     { $eq: ["$chi", "$$event.vhi"] },
     *                                     { $gt: ["$alias", null] }, // not null
     *                                 ]
     *                             }
     *                         }
     *                     },
     *                     {
     *                         $project: {
     *                             _id: 0,
     *                             alias: 1
     *                         }
     *                     }
     *                 ],
     *                 as: "view"
     *             }
     *         },
     *         {
     *             $addFields: {
     *                 alias: "$view.alias"
     *             }
     *         },
     *         {
     *             $project: {
     *                 alias: 1,
     *                 view_id: "$vi",
     *                 ...
     *             }
     *         },
     *     ]
     * )
     */
    private fun _getMongoQueryEventsOfSession(
        sessionId: String,
        appIdList: List<String>,
        skip: Int,
        limit: Int
    ): Pair<String, List<D>> {

        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
        val aggrQuery = mutableListOf(
            this._getMatchQuery(sessionId),
            D("\$sort", D("ts", 1))
        )
        if (skip >= 0) {
            aggrQuery += D("\$skip", skip)
        }
        if (limit > 0) {
            aggrQuery += D("\$limit", limit)
        }
        aggrQuery += this._getLookupQuery(appIdInObjIds)
        aggrQuery += D("\$addFields", D("alias", "\$view.alias"))
        aggrQuery += this._getProject()

        return EventService.COLLECTION_NAME to aggrQuery
    }


    /**
     * {
     *     $match: {
     *         "_id.si": "ffce62fa592e028a26a1e5bf12550597a8e36792"
     *     }
     * },
     */
    fun _getMatchQuery(sessionId: String): D {

        return D("\$match", D("_id.si", sessionId))
    }

    /**
     * @query
     * {
     *     $lookup: {
     *         from: "view_list_pt24h",
     *         let: {
     *             event_vhi: "$vhi"
     *         },
     *         pipeline: [
     *             {
     *                 $match: {
     *                     $expr: {
     *                         $and: [
     *                             { $eq: ["$ai", ObjectId("000000000000000000000004")] },
     *                             { $eq: ["$vhi", "$$event_vhi"] },
     *                             { $gt: ["$alias", null] }, // not null
     *                         ]
     *                     }
     *                 }
     *             },
     *             {
     *                 $project: {
     *                     _id: 0,
     *                     alias: 1
     *                 }
     *             }
     *         ],
     *         as: "view"
     *     }
     * }
     *
     * @param appIdInObjId use only first element
     */
    fun _getLookupQuery(appIdInObjId: List<ObjectId>): D {
        val matchCond = mutableListOf(
            D("\$eq", listOf("\$vhi", "\$\$event_vhi")),
            D("\$gt", listOf("\$alias", null)), // not null
        )
        if (appIdInObjId.isNotEmpty()) {
            matchCond += D("\$eq", listOf("\$ai", appIdInObjId[0]))
        }

        return D(
            "\$lookup",
            D("from", ViewList.COLLECTION_NAME_PT24H)
                .append("let", D("event_vhi", "\$vhi"))
                .append(
                    "pipeline", listOf(
                        D(
                            "\$match",
                            D(
                                "\$expr",
                                D("\$and", matchCond)
                            )
                        ),
                        D(
                            "\$project",
                            D("_id", 0).append("alias", 1)
                        )
                    )
                ).append("as", "view")
        )
    }

    /**
     * {
     *     $project: {
     *         alias: 1,
     *         view_id: "$vi",
     *         ...
     *     }
     * },
     */
    fun _getProject(): D {
        val projectDoc = this._getProjectQueryEventOfSession()
        projectDoc.append("alias", 1)
        return D("\$project", projectDoc)
    }

    /**
     *
     * db.event.find({
     *     "_id.si": "ffce62fa592e028a26a1e5bf12550597a8e36792"
     * })
     *    .limit(10)
     *    .sort({ts:-1})
     *    .projection({
     *
     *    })
     *
     */
    private fun _getFindQueryEventOfSession(
        sessionId: String,
        limit: Int,
        sortCond: D?
    ): Publisher<D> {
        var findQuery = MongodbUtil.getCollection(EventService.COLLECTION_NAME)
            .find(D("_id.si", sessionId))
        if (limit >= 0) {
            findQuery = findQuery.limit(limit)
        }
        return findQuery.sort(sortCond ?: D(Protocol.ts, -1))
            .projection(this._getProjectQueryEventOfSession())
    }

    /**
     *
     * {
     *   "view_id": "$vi",
     *   "view_hash_id": "$vhi",
     *   ...
     * }
     */
    private fun _getProjectQueryEventOfSession(): D {
        val allResKeys = listOf(
            "view_id",
            "view_hash_id",
            "event_type",
            "time_stamp",
            "gesture_x",
            "gesture_y",
            "gesture_id",
            "gesture_end_x",
            "gesture_end_y",
            "gesture_vector",
            "object_id",
            "svi",
            "spx",
            "spy",
            "view_width",
            "view_height",
            "crash_id",
            "crash_type",
            "crash_message",
            "crash_stacktrace"
        )
        val res = D(
            "_id", 1
        )
        for (key in allResKeys) {
            res.append(key, "\$${Protocol.getAbbreviation(key)}")
        }

        return res
    }
}