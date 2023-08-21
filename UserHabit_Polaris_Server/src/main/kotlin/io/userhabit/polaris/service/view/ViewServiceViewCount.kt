package io.userhabit.polaris.service.view

import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.Protocol
import org.bson.types.ObjectId
import org.bson.Document as D
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers

class ViewServiceViewCount {
    private val log = Loggers.getLogger(this.javaClass)
    private val _VHI_SESSIONTSTART = 74180013
    private val _VHI_SESSIONTEND = -1671032428

    fun getMongoResStreamTotalCount(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
    ): Mono<List<D>> {
        return appIdStream.flatMap { ownedAppIdList ->

            val safeAppList = this._getSafeAppList(ownedAppIdList, appIdList)
            val (coll, query) = this._getMongoQueryTotalViewCount(safeAppList, versionList)

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
     * db.view_list_pt24h.aggregate([
     *   {
     *       $match: {
     *           ai: { $in: [ObjectId("000000000000000000000004")] },
     *           av: { $in: ["1.0.1"] },
     *       }
     *   },
     *   {
     *       $group:{
     *           _id:{
     *               vhi: "$vhi"
     *           }
     *       }
     *   },
     *   {$count: "view_count"}
     * ])
     *
     *
     */
    private fun _getMongoQueryTotalViewCount(
        appIdList: List<String>,
        versions: List<String>
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return ViewList.COLLECTION_NAME_PT24H to listOf(
            this._getPipeMatchQuery(appIdInObjIds, versions),
            this._getPipeGroupByVhi(),
            D("\$count", "view_count")
        )
    }

    /**
     * {
     *   $match: {
     *     ai: { $in: [ObjectId("000000000000000000000004")] },
     *     av: { $in: ["1.0.1"] },
     *     // exclude session_start and session_end
     *     vhi: {$nin:[74180013, -1671032428]}
     *   }
     * },
     */
    private fun _getPipeMatchQuery(
        appIdList: List<ObjectId>,
        versionList: List<String>,
    ): D {
        val matchCod = D(
            Protocol.ai, D("\$in", appIdList)
        )
            .append(Protocol.vhi, D("\$nin", listOf(_VHI_SESSIONTSTART, _VHI_SESSIONTEND)))
        if (versionList.isNotEmpty()) {
            matchCod.append(Protocol.av, D("\$in", versionList))
        }

        return D("\$match", matchCod)
    }

    /**
     * {
     *   $group:{
     *     _id:{
     *       vhi: "$vhi"
     *     }
     *   }
     * },
     */
    private fun _getPipeGroupByVhi(): D {

        return D(
            "\$group", D(
                "_id", D("vhi", "\$${Protocol.vhi}")
            )
        )
    }


}