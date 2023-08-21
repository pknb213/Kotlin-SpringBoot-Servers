package io.userhabit.polaris.service.heatmap


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

class HeatmapServiceFirstLastTap {

    private val log = Loggers.getLogger(this.javaClass)

    companion object {
        const val TYPE_FIRSTTAP = "first_tap"
        const val TYPE_LASTTAP = "last_tap"
    }

    /**
     * @return
     * [
     *    {
     *      "hx" : 0.9,
     *      "hy" : 0.1,
     *      "count" : 1
     *    },
     *    {...},
     * ]
     */
    fun getMongoResStreamFirstLastTap(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        fromDate: String,
        toDate: String,
        viewList: List<String>,
        orientation: Int,
        type: String
    ): Mono<List<D>> {

        return appIdStream.flatMap { ownedAppIdList ->
            val safeAppList: List<String> = this._getSafeAppList(ownedAppIdList, appIdList)

            val (coll, query) = this._getMongoQueryFirstLastTap(
                fromDate, toDate,
                safeAppList, versionList, viewList, orientation, type,
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
     * db.indicator_heatmap_by_view_pt10m.aggregate([
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
     *       "$eq": 1101
     *     },
     *     "fevent": true,
     *     "vo": {
     *       "$eq": 1
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
     *     }
     *   }
     * },
     * {
     *     $group:{
     *         _id: {
     *             "vhi": "$vhi",
     *             "hx": "$hx",
     *             "hy": "$hy",
     *         },
     *         count: {$sum: "$count"},
     *
     *     }
     * },
     * {
     *     $project: {
     *         _id: 0,
     *         hx: "$_id.hx",
     *         hy: "$_id.hy",
     *         count: "$count"
     *     }
     * }
     *
     *
     *
     */
    private fun _getMongoQueryFirstLastTap(
        fromDate: String,
        toDate: String,
        appIdList: List<String>,
        versionList: List<String>,
        viewIdList: List<String>,
        orientation: Int,
        firstLast: String,
    ): Pair<String, List<D>> {

        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
        val viewListInInt = viewIdList.map { it.toInt() }

        return IndicatorHeatmapByView.COLLECTION_NAME_PT10M to listOf(
            this._getMatchQuery(fromDate, toDate, appIdInObjIds, versionList, viewListInInt, orientation, firstLast),
            this._getGroupByVhiHxHyQuery(),
            this._getProjectQueryForNodes(),
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
     *     "fevent": true,
     *     "t": {
     *       "$eq": 1101
     *     },
     *     "vo": {
     *       "$eq": 1
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
     *     }
     *   }
     * },
     */
    private fun _getMatchQuery(
        fromDate: String,
        toDate: String,
        appIdInObjIds: List<ObjectId>,
        versionList: List<String>,
        viewIdList: List<Int>,
        orientation: Int,
        firstLast: String,
    ): D {

        val firstLastField = mapOf(
            TYPE_FIRSTTAP to "fevent",
            TYPE_LASTTAP to "levent"
        )
        val matchQuery = D()
            .append(Protocol.stz,
                D(
                    "\$gte", Date.from(Instant.parse(fromDate)))
                    .append("\$lte", Date.from(Instant.parse(toDate))))
            .append(Protocol.ai, D("\$in", appIdInObjIds)) // Todo : Token 체크
            .append(Protocol.t, EventType.REACT_TAP)
            .append(Protocol.vo, D("\$eq", orientation))
            .append(firstLastField.get(firstLast), true)


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
     *     $group:{
     *         _id: {
     *             "vhi": "$vhi",
     *             "hx": "$hx",
     *             "hy": "$hy",
     *         },
     *         count: {$sum: "$count"},
     *     }
     * },
     */
    private fun _getGroupByVhiHxHyQuery(): D {
        return D("\$group", D()
            .append("_id",
                D(Protocol.vhi, "\$${Protocol.vhi}")
                    .append("hx", "\$hx")
                    .append("hy", "\$hy")
            )
            .append("count", D("\$sum", "\$count"))
        )
    }

    /**
     * {
     *     $project: {
     *         _id: 0,
     *         hx: "$_id.hx",
     *         hy: "$_id.hy",
     *         count: "$count"
     *     }
     * }
     */
    private fun _getProjectQueryForNodes(): D {
        return D("\$project", D()
            .append("_id", 0)
            .append("hx", "\$_id.hx")
            .append("hy", "\$_id.hy")
            .append("count", "\$count")
        )
    }


}
