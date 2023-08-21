package io.userhabit.polaris.service.session

import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.Protocol
import io.userhabit.polaris.service.SessionService
import org.bson.Document as D
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers


class SessionServiceSessionInfo {

    private val log = Loggers.getLogger(this.javaClass)

    /**
     * @query
     *   db.session.find({
     *       di: "b25a18f448e6ecb384da7c92269271070ab62cc6",
     *       ai: ObjectId("000000000000000000000004"),
     *       av: "1.0.1"
     *   }).sort({st: 1}).limit(1).projection({
     *       _id: 0,
     *       "device_name": "$dn",
     *       "device_zone": "$dz",
     *       "session_exp": "$se",
     *       "device_w": "$dw",
     *       "device_h": "$dh",
     *       "session_time": {"$dateToString" : {date: "$st"}},
     *       "device_lang": "$dl"
     *   })
     */
    fun getMongoResStreamFirstSession(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        deviceId: String
    ): Mono<List<D>> {
        return this._getMongoResStreamSession(appIdStream, appIdList, versionList, deviceId, D(Protocol.st, 1))
    }

    /**
     * @query
     *   db.session.find({
     *       di: "b25a18f448e6ecb384da7c92269271070ab62cc6",
     *       ai: ObjectId("000000000000000000000004"),
     *       av: "1.0.1"
     *   }).sort({st: -1}).limit(1).projection({
     *       _id: 0,
     *       "device_name": "$dn",
     *       "device_zone": "$dz",
     *       "session_exp": "$se",
     *       "device_w": "$dw",
     *       "device_h": "$dh",
     *       "session_time": {"$dateToString" : {date: "$st"}}
     *   })
     */
    fun getMongoResStreamLastSession(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        deviceId: String
    ): Mono<List<D>> {
        return this._getMongoResStreamSession(appIdStream, appIdList, versionList, deviceId, D(Protocol.st, -1))
    }

    fun _getMongoResStreamSession(
        appIdStream: Mono<List<String>>,
        appIdList: List<String>,
        versionList: List<String>,
        deviceId: String,
        sortCond: D
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

            val findQuery = this._getDeviceIdFindCond(safeAppList, versionList, deviceId)
            Flux.from(
                MongodbUtil.getCollection(SessionService.COLLECTION_NAME)
                    .find(findQuery)
                    .sort(sortCond)
                    .limit(1)
                    .projection(this._getProjLastSessionInfo())
            )
                .collectList()
                .doOnError {
                    log.info(Util.toPretty(findQuery))
                }
        }
    }

    /**
     *  {
     *      "di": "b25a18f448e6ecb384da7c92269271070ab62cc6",
     *      "ai": ObjectId("00000000000000000000000000000004"),
     *      "av": "1.0.1",
     *  }
     */
    private fun _getDeviceIdFindCond(
        appIdList: List<String>,
        versionList: List<String>,
        deviceId: String
    ): D {
        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
        val matchCond = D(Protocol.di, deviceId).append(Protocol.ai, D("\$in", appIdInObjIds))
        if (versionList.isNotEmpty()) {
            matchCond.append(Protocol.av, D("\$in", versionList))
        }
        return matchCond
    }

    /**
     *  {
     *       _id: 0,
     *       "device_name": "$dn",
     *       "device_zone": "$dz",
     *       "session_exp": "$se",
     *       "device_w": "$dw",
     *       "device_h": "$dh",
     *       "session_time": {"$dateToString" : {date: "$st"}},
     *       "device_lang": "$dl"
     *   }
     */
    private fun _getProjLastSessionInfo(): D {

        return D("_id", 0)
            .append(Protocol.dnK, "\$${Protocol.dn}")
            .append(Protocol.dzK, "\$${Protocol.dz}")
            .append(Protocol.seK, "\$${Protocol.se}")
            .append(Protocol.dwK, "\$${Protocol.dw}")
            .append(Protocol.dhK, "\$${Protocol.dh}")
            .append(Protocol.stK, D("\$dateToString", D("date", "\$${Protocol.st}")))
            .append(Protocol.dlK, "\$${Protocol.dl}")
    }
}