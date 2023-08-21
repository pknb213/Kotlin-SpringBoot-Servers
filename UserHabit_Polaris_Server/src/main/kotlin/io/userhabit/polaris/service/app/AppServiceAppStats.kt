package io.userhabit.polaris.service.app

import io.userhabit.batch.indicators.IndicatorAllByApp
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.Protocol
import org.bson.types.ObjectId
import org.bson.Document as D
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers


class AppServiceAppStats {

    private val log = Loggers.getLogger(this.javaClass)

    fun getMongoResStreamAppInfo(
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

            val (coll, query) = this._getMongoQueryAppInfo(safeAppList, versionList)

            Flux.from(MongodbUtil.getCollection(coll).aggregate(query))
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(query))
                }
        }
    }

    /**
     * db.indicator_all_by_app_pt24h.aggregate(
     * [
     *   {
     *     "$match": {
     *       "ai": {
     *         "$in": [
     *           ObjectId("000000000000000000000004")
     *         ]
     *       },
     *       "av": {
     *         "$in": [
     *           "1.0.1"
     *         ]
     *       }
     *     }
     *   },
     *   {
     *     "$group": {
     *       "_id": {
     *         "ai": "$ai",
     *         "av": "$av"
     *       },
     *       "dt": {
     *         "$sum": "$dt"
     *       },
     *       "sco": {
     *         "$sum": "$sco"
     *       },
     *       "vco": {
     *         "$sum": "$vco"
     *       },
     *     }
     *   }
     * ])
     */
    private fun _getMongoQueryAppInfo(
        appIdList: List<String>,
        versions: List<String>,
    ): Pair<String, List<D>> {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

        return IndicatorAllByApp.COLLECTION_NAME_PT24H to listOf(
            this._getPipeMatchQuery(appIdInObjIds, versions),
            this._getPipeGroupByAppIdVer(),
        )
    }

    private fun _getPipeMatchQuery(
        appIdList: List<ObjectId>,
        versionList: List<String>,
    ): D {
        val matchCod = D(
            Protocol.ai, D("\$in", appIdList)
        )
        if (versionList.isNotEmpty()) {
            matchCod.append(Protocol.av, D("\$in", versionList))
        }

        return D("\$match", matchCod)
    }

    private fun _getPipeGroupByAppIdVer(): D {

        return D(
            "\$group",
            D(
                "_id",
                D(Protocol.ai, "\$${Protocol.ai}")
                    .append(Protocol.av, "\$${Protocol.av}"),
            )
                .append(Protocol.dtK, D("\$sum", "\$${Protocol.dt}"))
                .append(Protocol.scoK, D("\$sum", "\$${Protocol.sco}"))
                .append(Protocol.vcoK, D("\$sum", "\$${Protocol.vco}"))
        )
    }

}