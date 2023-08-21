package io.userhabit.batch.common

import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoCollection
import io.userhabit.batch.BatchUtil
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import org.reactivestreams.Publisher
import org.bson.Document as D
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

class ExeButler {
    private val log = Loggers.getLogger(this.javaClass)
    private val collBatch = MongodbUtil.getCollection(BatchUtil.COLLECTION_NAME)

    fun run(
        batchName: Class<Any>,
        queryRaw: String,
        srcCollName: String,
        fromDt: ZonedDateTime,
        toDt: ZonedDateTime
    ): Mono<String> {

        val isManual = System.getProperty("sun.java.command") == "io.userhabit.batch.BatchUtil"  // 수동 배치 실행을 위해
        val query = queryRaw.trimIndent().replace(Regex("[#]"), "\\$")
        val filter = D(
            "_id", D("from_dt", Date.from(fromDt.toInstant()))
                .append("to_dt", Date.from(toDt.toInstant()))
                .append("name", this._getName(batchName, fromDt, toDt))
        )
        val readyOrFailedStatusUpdateStream =
            this._getReadyOrFailedStatusUpdateStream(filter, batchName, fromDt, toDt, query, isManual)
        val statusRunningUpdateStream =
            this._getStatusRunningUpdateStream(readyOrFailedStatusUpdateStream, filter)
        val aggregateStream = this._getAggregateStream(statusRunningUpdateStream, srcCollName, query)
        val statusDoneUpdateStream = this._getStatusDoneUpdateStream(aggregateStream, filter)

        return statusDoneUpdateStream
    }

    fun _getName(prefixClass: Class<Any>, fromDt: ZonedDateTime, toDt: ZonedDateTime): String {
        return prefixClass.let {
            "${it.enclosingClass.simpleName}.${it.enclosingMethod.name}(${
                Duration.between(
                    fromDt,
                    toDt
                )
            })"
        }
    }

    fun _getStatusDoneUpdateStream(aggregateStream: Mono<D>, filter: D): Mono<String> {
        val statusDoneUpdateStream = aggregateStream.flatMap { aggResDoc ->
            Mono.from(
                collBatch.updateOne(
                    filter,
                    D(
                        "\$set",
                        D("status", BatchUtil.STATUS_DONE).append("end_time", Date())
                    )
                )
            )
        }.onErrorResume { err ->
            log.error(err.toString(), err)
            // TODO: remove nested subscribe
            Mono.from(
                collBatch.findOneAndUpdate(
                    filter,
                    D(
                        "\$set", D("status", BatchUtil.STATUS_FAILED).append("end_time", Date())
                    ).append(
                        "\$push", D("errors", err.stackTrace.joinToString("\n"))
                    ),
                    FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
                ),
            ).subscribe()
            Mono.empty<UpdateResult>()
        }.map { updateRes ->
            updateRes.toString()
        }
        return statusDoneUpdateStream
    }

    /**
     * Run mongo updateOne query
     */
    fun _getStatusRunningUpdateStream(readyOrFailedStatusUpdateStream: Mono<D>, filter: D): Mono<UpdateResult> {
        val statusRunningUpdateStream = readyOrFailedStatusUpdateStream.flatMap { updatedDoc ->

            Mono.from(
                collBatch.updateOne(
                    filter,
                    D(
                        "\$set", D("status", BatchUtil.STATUS_RUNNING)
                            .append("start_time", Date())
                            .append("end_time", null)
                            .append("host", Util.hostName)
                    )

                )
            )
        }
        return statusRunningUpdateStream
    }

    /**
     * Run mongo aggregation query
     */
    fun _getAggregateStream(
        statusRunningUpdateStream: Mono<UpdateResult>,
        aggrColl: String,
        query: String
    ): Mono<D> {
        val targetColl = MongodbUtil.getCollection(aggrColl)
        val aggregateStream = statusRunningUpdateStream.flatMap { updateResult ->
            log.info(query)
//            val q = if (isManual) {
//                // TODO debug
//                log.info("${(updatedDoc["_id"] as Map<*, *>)["name"]}\ndb.${srcCollName}.aggregate(\n$query\n)")
//                query
//            } else {
//                updatedDoc["query"] as String
//            }
            Mono.from(
                targetColl
                    .aggregate(Util.jsonToList(query).map {
                        D.parse(Util.toJsonString(it))
                    })
                    .allowDiskUse(true)
            )
        }.defaultIfEmpty(D())
        return aggregateStream
    }

    fun _getReadyOrFailedStatusUpdateStream(
        filter: D,
        batchName: Class<Any>,
        fromDt: ZonedDateTime,
        toDt: ZonedDateTime,
        queryStr: String,
        isManual: Boolean
    ): Mono<D> {
        val statusUpdateStream = Mono.from(
            this._getUpdateStatusPub(filter, batchName, fromDt, toDt, queryStr)
        )
        val statusReadyOrFailedStream = statusUpdateStream.filter { updatedDoc ->
            this._getFilterStatusReadyOrFailed(updatedDoc.get("status") as String, isManual)
        }
        return statusReadyOrFailedStream
    }

    fun _getFilterStatusReadyOrFailed(status: String, isManual: Boolean): Boolean {
        return (status == BatchUtil.STATUS_READY || status == BatchUtil.STATUS_FAILED || isManual)
    }

    /**
     * db.collection.findOneAndUpdate( filter, update, options )
     * ex)
     * db.batch.findOneAndUpdate(
     *   {
     *     _id: {
     *       from_dt: ISODate('2022-02-01T10:00.000Z'),
     *       to_dt: ISODate('2022-02-01T10:00.000Z'),
     *       name: 'batch.name.cdfe.cofe',
     *     }
     *   },
     *   {
     *     $setOnInsert : {
     *       status: 'READY',
     *       query: <query_string_to_log>
     *     }
     *   },
     *   {
     *     upsert: true,
     *     returnNewDocument: true
     *   }
     * )
     *
     */
    fun _getUpdateStatusPub(
        filter: D,
        batchName: Class<Any>,
        fromDt: ZonedDateTime,
        toDt: ZonedDateTime,
        queryStr: String
    ): Publisher<D> {


        val update = D(
            "\$setOnInsert", D("status", BatchUtil.STATUS_READY).append("query", queryStr)
        )

        return collBatch.findOneAndUpdate(
            filter,
            update,
            FindOneAndUpdateOptions().upsert(true)
                .returnDocument(ReturnDocument.AFTER)
        )
    }


}