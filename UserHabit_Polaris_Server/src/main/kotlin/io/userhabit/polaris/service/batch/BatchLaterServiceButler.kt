package io.userhabit.polaris.service.batch

import io.userhabit.batch.BatchUtil
import io.userhabit.common.MongodbUtil
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import org.bson.Document as D


class BatchLaterServiceButler {

    private val log = reactor.util.Loggers.getLogger(this.javaClass)
    private val FIELD_EXETIME = "exetime"
    private val METHODNAME_TENMINUTES = "tenMinutes"
    private val METHODNAME_ONEDAY = "oneDay"
    private val onlyTenMinBatch = listOf<String>("HeatmapByViewBatch", "PreviewByCohort", "IpToIsoBatch")

// .concatWith(AllFlowBatch.tenMinutes(fromDtMinute, toDtMinute))
//                .concatWith(AllFlowBatch.oneDay(fromDtDay, toDtDay))

    /**
     * This gets the last exe time for the batches then,
     * run from that time and do until the current time
     *
     * @param batchList
     *   ["AllFlowBatch", ...]
     */
    fun runBatches(batchList: List<String>): Mono<List<String>> {

        val lastTimeStream = this._getTheLastExeTimeStream(batchList)
        return lastTimeStream.flatMap { lastTimeList ->
            this._callBatchMethodToNowStream(lastTimeList, batchList)
        }

    }

    /**
     *
     * This returns the most recent last time
     *
     * db.batch.find({
     *     "_id.name": { $in: ["ReachRateByScrollViewBatch.tenMinutes(PT10M)"]}
     * })
     *    .projection({
     *        _id: 0,
     *        "from" : "$_id.from_dt"
     *    })
     *    .sort({"_id.from_dt":-1})
     *    .limit(10)
     *
     * @param batchList
     *  ["ReachRateByScrollViewBatch", "ViewByViewBatch", ...]
     *
     *  @return
     *   [
     *     {
     *       "from" : ISODate("2022-05-08T09:40:00.000+09:00")
     *     },
     *     {
     *       "from" : ISODate("2022-05-08T09:30:00.000+09:00")
     *     },
     *     ...
     *   ]
     */
    fun _getTheLastExeTimeStream(batchList: List<String>): Mono<List<D>> {

        return Flux
            .from(
                MongodbUtil.getCollection(BatchUtil.COLLECTION_NAME).find(
                    D(
                        "_id.name", D(
                            "\$in", this._getAllBatchesNamesOf10Min(batchList)
                        )
                    )
                        .append("status", BatchUtil.STATUS_DONE)
                ).projection(
                    D("_id", 0).append(FIELD_EXETIME, "\$_id.from_dt")
                ).sort(
                    D("_id.from_dt", -1)
                ).limit(1)
            ).collectList()
    }

    /**
     * @param batchList
     *   ["AllFlowBatch", "ViewListBatch"]
     * @return
     *   ["AllFlowBatch.tenMinutes(PT10M)", "ViewListBatch.tenMinutes(PT10M)"]
     */
    fun _getAllBatchesNamesOf10Min(batchList: List<String>): List<String> {
        val ret = mutableListOf<String>()
        for (batch in batchList) {
            ret.add("$batch.tenMinutes(PT10M)")
        }
        return ret
    }


    fun _getZonedDateTime(ms: Long): ZonedDateTime {
        val instant = Instant.ofEpochMilli(ms)
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
    }

    /**
     * @param lastTime Should be rounded to 10 minutes
     *   Date("2022-05-08T09:40:00.000+09:00") is ok,
     *   but Date("2022-05-08T09:42:00.000+09:00") is NOT ok
     * @return
     *   [Date("2022-05-08T09:50:00.000+09:00", Date("2022-05-08T10:00:00.000+09:00", .... ]
     */
    fun _getExeTimeStream(lastTime: Date): Flux<Pair<ZonedDateTime, ZonedDateTime>> {
        var curTime = lastTime.time
        val endTime = Date().time

        val tenMin = 10 * 60 * 1000
        val exeTimes = mutableListOf<Pair<ZonedDateTime, ZonedDateTime>>()
        curTime += tenMin
        while (curTime + (tenMin) <= endTime) {
            val fromDt = this._getZonedDateTime(curTime)
            val toDt = this._getZonedDateTime(curTime + (tenMin))
            exeTimes.add(Pair(fromDt, toDt))
            curTime += tenMin   // 10 min
        }

        return Flux.fromIterable(exeTimes)
    }


    /**
     * This method assumes that all the batches have the same last times
     *
     * If the A batch has the time '2022-02-01T10:00:00Z'
     * and the B batch has the time '2022-02-01T10:10:00Z',
     * This method calls the batch method from the '2022-02-01T10:20:00Z',
     * just skip the '2022-02-01T10:10:00Z' of the A batch
     *
     */
    fun _callBatchMethodToNowStream(lastTimeList: List<D>, batchList: List<String>): Mono<List<String>> {
        val lastTime = _getLastTime(lastTimeList)
        val exeTimesStream = this._getExeTimeStream(lastTime)

        return exeTimesStream.concatMap {
            val from = it.first
            val to = it.second
            log.info("exeTimesStream - ${it.first.toString()}")
            Flux.fromIterable(batchList).concatMap { batchName ->
                log.info("batchList - ${batchName}")
                // return
                if (from.minute == 0 && !onlyTenMinBatch.contains(batchName)) {
                    // run both or run neither
                    this._callBatchMethodStream(batchName, from, to, METHODNAME_TENMINUTES)
                        .concatWith(this._callBatchMethodStream(batchName, from, from.plusDays(1), METHODNAME_ONEDAY))
                } else {
                    this._callBatchMethodStream(batchName, from, to, METHODNAME_TENMINUTES)
                }

            }
        }.collectList()
    }


    /**
     * @param batchName
     *   ["AllFlowBatch"]
     * @return
     *  if the param batchName is "AllFlowBatch",
     *    `AllFlowBatch.tenMinutes(from, to)` or `AllFlowBatch.oneDay(from, to)` is called
     */
    fun _callBatchMethodStream(
        batchName: String,
        from: ZonedDateTime,
        to: ZonedDateTime,
        methodName: String
    ): Mono<String> {
        val batchClass = Class.forName("io.userhabit.batch.$batchName")
        val batchInstance = batchClass.kotlin.objectInstance
        val batchMethod =
            batchClass.getDeclaredMethod(methodName, ZonedDateTime::class.java, ZonedDateTime::class.java)

        return batchMethod.invoke(batchInstance, from, to) as Mono<String>
    }

    fun _getLastTime(lastTimeList: List<D>): Date {
        if (lastTimeList.size <= 0) {
            return _getDaysAgo(1)
        }
        return lastTimeList[0].get(FIELD_EXETIME) as Date
    }

    fun _getDaysAgo(daysAgo: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) / 10 * 10);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)

        return calendar.time
    }

}
