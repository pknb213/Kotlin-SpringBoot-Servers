package io.userhabit.polaris.service.geoip

import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.FindPublisher
import io.userhabit.batch.IpToIsoBatch
import io.userhabit.common.MongodbUtil

import io.userhabit.polaris.service.SessionService

import org.bson.Document as D
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.time.Instant
import java.util.*

class GeoIpMapper {
    private val preloadedIps = mutableListOf<String>()
    private val NOTINRANGE = -2
    private val INRANGE = -1

    companion object {

    }

    fun updateCountryCodeOfSessionListStream(isoCode: String, fromDate: String, toDate: String): Mono<UpdateResult> {
        val countryCodeStream = this.getCountryIpAddrRangeStream(isoCode)
        val noCountryStream = this.getSessionsNoCountryCodeStream(fromDate, toDate)

        return countryCodeStream.flatMap { ipRanges ->
            noCountryStream.map { session ->
                Pair(this._bsearch(ipRanges, 0, ipRanges.size, session.get("ipi") as String), session.get("_id"))
            }.filter { pair ->
                pair.first == INRANGE
            }.map {
                it.second
            }.collectList().flatMap { sessionIdList ->
                // update session.ico
                Mono.from(
                    MongodbUtil.getCollection(SessionService.COLLECTION_NAME).updateMany(
                        D("_id", D("\$in", sessionIdList)),
                        D("\$set", D("ico", isoCode))
                    )
                )
            }
        }
    }

    /**
     * @param countryIsoCode
     *  ex: 'KR'
     */
    fun getCountryIpAddrRangeStream(countryIsoCode: String): Mono<List<String>> {

        if (preloadedIps.isNotEmpty()) {
            return Mono.just(preloadedIps)
        }

        return this._getIpRangeListStream(countryIsoCode)
    }


    /**
     * @param data
     *   ["10b0000", "10bffff", "1100000", ..., "fe65100", "fe651ff"]
     * @param target
     */
    fun _bsearch(data: List<String>, low: Int, high: Int, target: String): Int {
        if (high >= low) {
            val mid = (low + high) / 2 as Int
            if (target == data[mid]) {
                return mid
            } else if (target < data[mid]) {
                return _bsearch(data, low, mid - 1, target)
            } else {
                return _bsearch(data, mid + 1, high, target)
            }
        } else {
            // not matched
            if (high % 2 == 1) {
                return NOTINRANGE
            }
            return INRANGE
        }
    }

    /**
     * @return
     *  ["10b0000", "10bffff", "1100000", ..., "fe65100", "fe651ff"]
     */
    fun _getIpRangeListStream(countryIsoCode: String): Mono<List<String>> {
        return Flux.from(
            this._getIpRangeFromDb(countryIsoCode)
        )
            .collectList()
            .map { itemList ->

                for (item in itemList) {
                    preloadedIps.add(item.get("min") as String)
                    preloadedIps.add(item.get("max") as String)
                }

                preloadedIps
            }
    }

    /**
     * db.geolist_ip.find({
     *   iso_code: 'KR'
     * })
     * .projection({
     *   _id: false,
     *   min: true,
     *   max: true,
     * })
     * .sort({
     *   min: 1
     * })
     */
    fun _getIpRangeFromDb(countryIsoCode: String): FindPublisher<D> {

        return MongodbUtil.getCollection(IpToIsoBatch.COLLECTION_NAME)
            .find(
                D("iso_code", countryIsoCode)
            ).projection(
                D("_id", false)
                    .append("min", true)
                    .append("max", true)
            )
            .sort(
                D("min", 1)
            )
    }

    /**
     * db.session.find({
     *   st: {
     *       $gte: ISODate("2022-04-26"),
     *       $lt: ISODate("2022-04-27"),
     *   },
     *   ico: {$exists: false},
     * })
     * .project({
     *   _id: true,
     *   ipi: true,
     * })
     */

    fun getSessionsNoCountryCodeStream(fromDate: String, toDate: String): Flux<D> {

        return Flux.from(
            MongodbUtil.getCollection(SessionService.COLLECTION_NAME)
                .find(
                    D(
                        "st",
                        D("\$gte", Date.from(Instant.parse(fromDate)))
                            .append("\$lt", Date.from(Instant.parse(toDate)))
                    ).append(
                        "ico", D("\$exists", false)
                    )
                ).projection(
                    D(
                        "_id", true
                    )
                        .append("ipi", true)
                )
        )
    }
}

