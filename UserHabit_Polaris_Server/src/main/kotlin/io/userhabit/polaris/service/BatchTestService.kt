package io.userhabit.polaris.service

import com.mongodb.client.model.IndexOptions
import io.userhabit.batch.BatchUtil
import io.userhabit.batch.indicators.*
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.Protocol
import org.bson.types.ObjectId
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import reactor.util.Loggers
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Date
import org.bson.Document as D
import io.userhabit.polaris.EventType as ET
import io.userhabit.polaris.Protocol as P

object BatchTestService {
    private val log = Loggers.getLogger(this.javaClass)
    // @deprecated
    // lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>

    fun heatmapByScrollViewBatch(req: SafeRequest): Mono<Map<String, Any>>{
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val validator = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
        if (validator.isNotValid()) throw PolarisException.status400BadRequest(validator.toExceptionList())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        return AppService.getAppIdList(Protocol.uiK, req.getUserId())
            .flatMap {
                val matchQry = mapOf(
                    "\$or" to listOf(
                        mapOf(P.i to mapOf(
                            "\$gte" to from,
                            "\$lt" to to
                        )),
                        mapOf("\$and" to listOf(
                            mapOf(P.i to mapOf("\$exists" to false)),
                            mapOf(P.st to mapOf(
                                "\$gte" to from.minusDays(1),
                                "\$lt" to to.minusDays(1)
                            ))
                        ))
                    )
                )
                val lookupQry = mapOf(
                    "from" to "event",
                    "let" to mapOf(P.si to "\$_id"),
                    "pipeline" to listOf(mapOf(
                        "\$match" to mapOf(
                            "\$expr" to mapOf(
                                "\$and" to listOf(
                                    mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                                    mapOf("\$in" to listOf("\$t", listOf(ET.REACT_TAP, ET.REACT_DOUBLE_TAP, ET.REACT_LONG_TAP, ET.REACT_SWIPE, ET.NOACT_TAP))),
                                    mapOf("\$gt" to listOf("\$svi", null)),
                                    mapOf("\$gt" to listOf("\$spx", null)),
                                    mapOf("\$gt" to listOf("\$spy", null)),
                                    mapOf("\$gte" to listOf("\$spx", 0)),
                                    mapOf("\$gte" to listOf("\$spy", 0))
                                )
                            )
                        )
                    )),
                    "as" to "event"
                )
                val groupQry = mapOf(
                    "_id" to mapOf(
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "t" to "\$event.t",
                        "vhi" to "\$event.vhi",
                        "svi" to "\$event.svi",
                        "spx" to mapOf("\$multiply" to listOf(
                            mapOf("\$toInt" to mapOf("\$divide" to listOf(
                                mapOf("\$multiply" to listOf(
                                    "\$event.spx",
                                    mapOf("\$divide" to listOf(720, "\$dw"))
                                )), 50
                            ))), 50
                        )),
                        "spy" to mapOf("\$multiply" to listOf(
                            mapOf("\$toInt" to mapOf("\$divide" to listOf(
                                mapOf("\$multiply" to listOf(
                                    "\$event.spy",
                                    mapOf("\$divide" to listOf(720, "\$dw"))
                                )), 50
                            ))), 50
                        )),
                        P.si to mapOf("\$toDate" to mapOf("\$concat" to listOf(
                            mapOf("\$substr" to listOf(
                                mapOf("\$dateToString" to mapOf(
                                    "format" to "%Y-%m-%d %H:%M",
                                    "date" to "\$st"
                                )), 0, 15
                            )),
                            "0:00"
                        )))
                    ),
                    "count" to mapOf("\$sum" to 1)
                )
                val lookupQry2 = mapOf(
                    "from" to "app",
                    "localField" to "_id.ai",
                    "foreignField" to "_id",
                    "as" to "app"
                )
                val replaceQry = mapOf(
                    "\$mergeObjects" to listOf(
                        mapOf(
                            "ai" to "\$_id.ai",
                            "av" to "\$_id.av",
                            P.si to "\$_id.st",
                            "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                                "date" to "\$_id.st",
                                "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                            ))),
                            "stzd" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                                "format" to "%Y-%m-%d 00:00:00",
                                "date" to "\$_id.st",
                                "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                            ))),
                            "t" to "\$_id.t",
                            "vhi" to "\$_id.vhi",
                            "svi" to "\$_id.svi",
                            "spx" to "\$_id.spx",
                            "spy" to "\$_id.spy",
                            "bft" to mapOf("\$toDate" to from),
                            "count" to "\$count"
                        )
                    )
                )
                val mergeQry = mapOf(
                    "into" to IndicatorHeatmapByScrollView.COLLECTION_NAME_PT10M,
                    "on" to listOf("st", "ai", "av", "vhi", "t", "svi", "spx", "spy"),
                    "whenMatched" to listOf(
                        mapOf("\$addFields" to mapOf(
                            P.si to "\$st",
                            "ai" to "\$ai",
                            "av" to "\$av",
                            "vhi" to "\$vhi",
                            "svi" to "\$svi",
                            "t" to "\$t",
                            "spx" to "\$spx",
                            "spy" to "\$spy",
                            "stz" to "\$\$new.stz",
                            "stzd" to "\$\$new.stzd",
                            "bft" to "\$\$new.bft",
                            "count" to mapOf(
                                "\$sum" to listOf("\$count", "\$\$new.count")
                            )
                        ))
                    ),
                    "whenNotMatched" to "insert"
                )

                val pipeline = listOf(
                    D().append("\$match", matchQry),
                    D().append("\$lookup", lookupQry),
                    D().append("\$unwind", "\$event"),
                    D().append("\$group", groupQry),
                    D().append("\$sort", mapOf("_id.spy" to -1)),
                    D().append("\$lookup", lookupQry2),
                    D().append("\$replaceWith", replaceQry),
                    D().append("\$merge", mergeQry)
                )
                Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
                    pipeline
                )).collectList()
                    .map {
                        println("Doc >> $it")
                        println("Cnt >> ${it.count()}")
                        Status.status200Ok(it)
                    }
            }
    }

    fun reachRateByScrollViewBatch(req: SafeRequest): Mono<Map<String, Any>>{
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val validator = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
        if (validator.isNotValid()) throw PolarisException.status400BadRequest(validator.toExceptionList())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        return AppService.getAppIdList(Protocol.uiK, req.getUserId())
            .flatMap {
                Flux.from(MongodbUtil.getCollection("app").aggregate(
                    listOf(
                        /*D("\$match", mapOf("\$expr" to mapOf("\$and" to listOf(
//                            mapOf("test" to listOf("\$gt", null)),
//                            mapOf("test2" to listOf("\$gt", null))
                            mapOf("\$gt" to listOf("test", null)),
                            mapOf("\$gt" to listOf("test2", null))
                        ))))*/
                    D("\$lookup",  mapOf(
                        "from" to "member",
                        "let" to mapOf("mid" to "\$member_id"),
                        "pipeline" to listOf(
                            D("\$match", mapOf("\$expr" to mapOf("\$and" to listOf(
//                            mapOf("test" to listOf("\$gt", null)),
//                            mapOf("test2" to listOf("\$gt", null))
                                mapOf("\$in" to listOf("\$_id", "\$\$mid")),
                                mapOf("\$ifNull" to listOf(mapOf(
                                    "\$gt" to listOf("\$test", null)),
                                    "Empty"
                                ))
                            ))))
                        ),
                        "as" to "memberCollection"
                    )))
                )).collectList()
                    .map {
                        println("DDoc >> $it")
                        println("CCnt >> ${it.count()}")
                        Status.status200Ok(it)
                    }
            }
    }
    // -------- 위에는 결과 값이 컬렉션에 적재되지 않는 상황 -------------
    fun viewListTenMinBatch(req: SafeRequest): Mono<Map<String, Any>>{
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val validator = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
        if (validator.isNotValid()) throw PolarisException.status400BadRequest(validator.toExceptionList())

//        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
//        val from = LocalDate.parse(fromDate, formatter)
//        val to = LocalDate.parse(toDate, formatter)
        
        return AppService.getAppIdList(Protocol.uiK, req.getUserId())
            .flatMap {
                val matchQry = mapOf(
                    "\$or" to listOf(
                        mapOf(P.i to mapOf(
                            "\$gte" to mapOf("\$date" to fromDate.format(BatchUtil.toDateFormater)),
                            "\$lt" to mapOf("\$date" to toDate.format(BatchUtil.toDateFormater))
                        )),
                        mapOf("\$and" to listOf(
                            mapOf(P.i to mapOf("\$exists" to false)),
                            mapOf(P.st to mapOf(
                                "\$gte" to mapOf("\$date" to fromDate.format(BatchUtil.toDateFormater)),
                                "\$lt" to mapOf("\$date" to toDate.format(BatchUtil.toDateFormater)) //.minusDays(1)
                            ))
                        ))
                    )
                )
                val lookupQry = mapOf(
                    "from" to "event",
                    "let" to mapOf(P.si to "\$_id"),
                    "pipeline" to listOf(mapOf("\$match" to mapOf("\$expr" to mapOf(
                        "\$and" to listOf(
                            mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                            mapOf("\$in" to listOf("\$t", listOf(ET.VIEW_START, ET.APP_START, ET.APP_END)))
                        )
                    )))),
                    "as" to "event"
                )
                val groupQry = mapOf(
                    "_id" to mapOf(
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "vi" to "\$event.vi",
                        "vhi" to "\$event.vhi",
                        P.si to mapOf("\$toDate" to mapOf("\$concat" to listOf(
                            mapOf("\$substr" to listOf(mapOf("\$dateToString" to mapOf(
                                "format" to "%Y-%m-%d %H:%M",
                                "date" to "\$st"
                            )), 0, 15)),
                            "0:00"
                        )))
                    )
                )
                val add = mapOf("_id.ai" to mapOf("\$toObjectId" to "\$_id.ai"))
                val lookupQry2 = mapOf(
                    "from" to "app",
                    "localField" to "_id.ai",
                    "foreignField" to "_id",
                    "as" to "app"
                )
                val replaceQry = mapOf(
                    "\$mergeObjects" to listOf(mapOf(
                        P.si to "\$_id.st",
                        "ai" to "\$_id.ai",
                        "av" to "\$_id.av",
                        "vi" to "\$_id.vi",
                        "vhi" to "\$_id.vhi",
                        "test3" to "\$app",
                        "test2" to "\$app.time_zone",
                        "test" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0)),
                        "stz" to mapOf("\$dateToString" to mapOf(
                            "date" to "\$_id.st",
                            "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                        )),
//                        "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
//                            "date" to "\$_id.st",
//                            "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
//                        ))),
                        "stzd" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                            "format" to "%Y-%m-%d 00:00:00",
                            "date" to "\$_id.st",
                            "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                        ))),
                        "bft" to mapOf("\$toDate" to mapOf("\$date" to fromDate.format(BatchUtil.toDateFormater)))
                    ))
                )
                val mergeQry = mapOf(
                        "into" to ViewList.COLLECTION_NAME_PT10M,
//                        "into" to "b", // Todo : 컬렉션이 존재하지 않거나 비어있으면 51183 에러 발생
                    "on" to listOf("st", "ai", "av", "vhi"),
                    "whenMatched" to "merge",
                    "whenNotMatched" to "insert"
                )

                val pipeline = listOf(
                    D().append("\$match", matchQry),
                    D().append("\$lookup", lookupQry),
                    D().append("\$unwind", "\$event"),
                    D().append("\$group", groupQry),
                    D().append("\$addFields", add),
                    D().append("\$lookup", lookupQry2),
                    D().append("\$replaceWith", replaceQry),
                    D().append("\$merge", mergeQry)
                )
                Flux
                    .from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
                        pipeline
                    ))
                    .collectList()
                    .map {
                        println("Doc => $it")
                        println("Cnt => ${it.count()}")
                        Status.status200Ok(it)
                    }
            }
    }

    fun objectListBatch(req: SafeRequest): Mono<Map<String, Any>> {
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val validator = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
        if (validator.isNotValid()) throw PolarisException.status400BadRequest(validator.toExceptionList())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)
        return AppService.getAppIdList(Protocol.uiK, req.getUserId())
            .flatMap {
                val matchQry = mapOf(
                    "\$or" to listOf(
                        mapOf(P.i to mapOf(
                            "\$gte" to from,
                            "\$lt" to to
                        )),
                        mapOf("\$and" to listOf(
                            mapOf(P.i to mapOf("\$exists" to false)),
                            mapOf(P.st to mapOf(
                                "\$gte" to from.minusDays(1),
                                "\$lt" to to.minusDays(1)
                            ))
                        ))
                    )
                )
                val lookupQry = mapOf(
                    "from" to "event",
                    "let" to mapOf(P.si to "\$_id"),
                    "pipeline" to listOf(mapOf("\$match" to mapOf("\$expr" to
                            mapOf("\$and" to listOf(
                                mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                                mapOf("\$eq" to listOf("\$t", ET.REACT_TAP)),
                                mapOf("\$gt" to listOf("\$oi", null))
                            ))
                    ))),
                    "as" to "event"
                )
                val groupQry = mapOf(
                    "_id" to mapOf(
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "vhi" to "\$event.vhi",
                        "oi" to "\$event.oi",
                        P.si to mapOf("\$toDate" to mapOf("\$concat" to listOf(
                            mapOf("\$substr" to listOf(
                                mapOf("\$dateToString" to mapOf(
                                    "format" to "%Y-%m-%d %H:%M",
                                    "date" to "\$st"
                                )), 0, 15
                            )),
                            "0:00"
                        )))
                    )
                )
                val lookupQry2 = mapOf(
                    "from" to "app",
                    "localField" to "_id.ai",
                    "foreignField" to "_id",
                    "as" to "app"
                )
                val replaceWithQry = mapOf(
                    "\$mergeObjects" to listOf(mapOf(
                        P.si to "\$_id.st",
                        "ai" to "\$_id.ai",
                        "av" to "\$_id.av",
//                        "vi" to "\$event.vi", // Todo : 기존 쿼리에 없음
                        "oi" to "\$_id.oi",
                        "vhi" to "\$_id.vhi",
                        "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                            "date" to "\$_id.st",
                            "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                        ))),
                        "stzd" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                            "format" to "%Y-%m-%d 00:00:00",
                            "date" to "\$_id.st",
                            "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                        ))),
                        "bft" to mapOf("\$toDate" to from)
                    ))
                )
                val mergeQry = mapOf(
                    "into" to ObjectList.COLLECTION_NAME_PT10M,
//                    "into" to "a", // Todo : 컬렉션이 존재하지 않거나 비어있으면 51183 에러 발생
                    "on" to listOf("st", "ai", "av", "vhi", "oi"),
                    "whenMatched" to "merge",
                    "whenNotMatched" to "insert"
                )
                val pipeline = listOf(
                    D().append("\$match", matchQry),
                    D().append("\$lookup", lookupQry),
                    D().append("\$unwind", "\$event"),
                    D().append("\$group", groupQry),
                    D().append("\$lookup", lookupQry2),
                    D().append("\$replaceWith", replaceWithQry),
                    D().append("\$merge", mergeQry),
                )
                Mono.from(MongodbUtil.getCollection("a").createIndex(
                    D(mapOf("id" to 1, "member_id" to 1)),
                    IndexOptions().unique(true)
                )).map { println(">>>> $it") }.subscribe()


                Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
                    pipeline
                )).collectList()
                    .map {
                        println("Doc >> $it")
                        println("Cnt >> ${it.count()}")
                        Status.status200Ok(it)
                    }
            }
    }

    fun test2(req: SafeRequest): Mono<Map<String, Any>> {
        val testList = listOf(
            mapOf("a" to 10), mapOf("b" to 1), mapOf("c" to 4),
            mapOf("a" to 9), mapOf("b" to 3), mapOf("c" to 4),
            mapOf("a" to 8), mapOf("b" to 5), mapOf("c" to 7),
            mapOf("a" to 6), mapOf("b" to 6), mapOf("c" to 7),
            mapOf("a" to 4), mapOf("b" to 8), mapOf("c" to 7),
        )
        val fieldList = listOf("a", "b", "c")
        return Flux
            .from(MongodbUtil.getCollection("A")
                .find()
                .projection(fieldList.fold(D()){
                    acc: D, s: String ->
                    acc.append(s, 1)
                })
            )
            .collectList()
            .map { mapOf("test" to it) }
    }

    fun test(req: SafeRequest): Mono<Map<String, Any>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val pipeline = listOf(
            D(mapOf(
                "\$match" to mapOf(
                    "bft" to mapOf(
                        "\$gte" to LocalDateTime.parse("2021-10-01T00:00:00Z", formatter).minusMonths(2),
                        "\$lt" to LocalDateTime.parse("2021-12-31T00:00:00Z", formatter)
                    )
                )
            )),
            D(mapOf(
                "\$group" to mapOf(
                    "_id" to mapOf(
                        "stzd" to "\$stzd",
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "vhi" to "\$vhi"
                    )
                )
            )),
            D(mapOf(
                "\$lookup" to mapOf(
                    "from" to ViewList.COLLECTION_NAME_PT10M,
                    "let" to mapOf("stzd" to "\$_id.stzd", "ai" to "\$_id.ai", "av" to "\$_id.av", "vhi" to "\$_id.vhi"),
                    "pipeline" to listOf(
                        mapOf(
                            "\$match" to mapOf(
                                "\$expr" to mapOf(
                                    "\$and" to listOf(
                                        mapOf("\$eq" to listOf("\$stzd", "\$\$_id.stzd")),
                                        mapOf("\$eq" to listOf("\$ai", "\$\$_id.ai")),
                                        mapOf("\$eq" to listOf("\$av", "\$\$_id.av")),
                                        mapOf("\$eq" to listOf("\$vhi", "\$\$_id.vhi"))
                                    )
                                )
                            )
                        )
                    ),
                    "as" to "data"
                )
            ))
        )
        val coll = ViewList.COLLECTION_NAME_PT10M
        println("$pipeline, $coll")
        return Flux
            .from(MongodbUtil.getCollection(coll).aggregate(pipeline))
            .map {
                println("Doc >>> $it")
                it
            }
            .collectList()
            .map {
                mapOf("test" to it.count())
            }
    }

    fun test3(req: SafeRequest): Mono<Map<String, Any>> {
        val zdt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zdt.minusDays(30)
        val toDt = zdt.plusDays(30)
        println(">> $fromDt ${fromDt.javaClass}")
        println(">> ${fromDt.toString()} ${fromDt.toString().javaClass}")
        val qry = """
[
   {"#match": 
        {P.i: { 
            "#gte" : {"#date": "${fromDt.format(BatchUtil.toDateFormater)}"},
            "#lt" : {"#date": "${toDt.format(BatchUtil.toDateFormater)}"}
        }}            
   }
]
        """.trimIndent().replace(Regex("[#]"), "\\$")
        Util.jsonToList(qry).map {
            println(">>> Date Formater : $it")
            val parseIt = D.parse(Util.toJsonString(it))
            println(">>> Parsing : $parseIt")
        }
        val qry2 = """
[
   {"#match": 
        {P.i: { 
            "#gte" : {"#date": "${fromDt.toInstant()}"},
            "#lt" : "${toDt.toInstant()}"
        }}            
   }
]
        """.trimIndent().replace(Regex("[#]"), "\\$")
        Util.jsonToList(qry2).map {
            println(">>> Date toInstant : $it")
            val parseIt = D.parse(Util.toJsonString(it))
            println(">>> Parsing : $parseIt")
        }
        val qry3 = """
    {"#match": 
        {P.i: { 
            "#gte" : ISODate("${fromDt}"),
            "#lt" : ISODate("${toDt}")
        }}
    }
        """.trimIndent().replace(Regex("[#]"), "\\$")
        println(">>> ISO Date : $qry3")
//        Util.jsonToList(qry3)
        val parseIt = D.parse(qry3) // Todo: Util 내부 함수들이 ISODate 메서드의 json str인지 확인하기 때문에 에러가 발생하는 것 같다.
        println(">>> Parsing : $parseIt")

        return Mono.just(mapOf("Test" to 1))
    }
    fun test4(req: SafeRequest): Mono<Map<String, Any>> {
        val zdt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zdt.minusDays(30)
        val toDt = zdt.plusDays(30)
        val SESSION_START_HASH_ID = 74180013
        val SESSION_END_HASH_ID = -1671032428

        val match = D(mapOf("\$match" to mapOf(
            "\$or" to listOf(
                mapOf(P.i to mapOf(
                    "\$gte" to fromDt.toInstant(),
                    "\$lt" to toDt.toInstant()
                )),
                mapOf("\$and" to listOf(
                    mapOf(P.i to mapOf("\$exists" to false)),
                    mapOf(P.st to mapOf(
                        "\$gte" to fromDt.minusDays(1).toInstant(),
                        "\$lt" to toDt.minusDays(1).toInstant()
                    ))
                ))
            )
        )))
        val lookup = D(mapOf("\$lookup" to mapOf(
            "from" to "event",
            "let" to mapOf(P.si to "\$_id"),
            "pipeline" to listOf(
                mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                    mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                    mapOf("\$in" to listOf("\$t", listOf(ET.VIEW_START, ET.APP_START, ET.APP_END)))
                ))))
            ),
            "as" to "flow"
        )))
        val addFields = D(mapOf("\$addFields" to mapOf(
            "flow" to mapOf(
                "\$map" to mapOf(
                    "input" to "\$flow",
                    "as" to "row",
                    "in" to mapOf(
                        "bvhi" to mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf(
                                mapOf("\$add" to listOf(
                                    mapOf("\$indexOfArray" to listOf("\$flow", "\$\$row")),
                                    -1
                                )),
                                -1
                            )),
                            SESSION_START_HASH_ID,
                            mapOf("\$arrayElemAt" to listOf(
                                "\$flow.vhi",
                                mapOf("\$indexOfArray" to listOf(
                                    "\$flow", "\$\$row"
                                ))
                            ))
                        )),
                        "vhi" to mapOf(
                            "\$arrayElemAt" to listOf(
                                "\$flow.vhi",
                                mapOf("\$indexOfArray" to listOf(
                                    "\$flow", "\$\$row"
                                ))
                            )
                        ),
                        "avhi" to mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf(
                                mapOf(
                                    "\$add" to listOf(
                                        mapOf(
                                            "\$indexOfArray" to listOf("\$flow", "\$\$row")
                                        ),
                                        1
                                    )
                                ),
                                mapOf("\$size" to "\$flow")
                            )),
                            SESSION_END_HASH_ID,
                            mapOf("\$arrayElemAt" to listOf(
                                "\$flow.vhi",
                                mapOf(
                                    "\$add" to listOf(
                                        mapOf(
                                            "\$indexOfArray" to listOf("\$flow", "\$\$row")
                                        ),
                                        1
                                    )
                                )
                            ))
                        ))
                    )
                )
            )
        )))
        val unwind = D(mapOf(
            "\$unwind" to mapOf("path" to "\$flow")
        ))
        val match2 = D(mapOf(
            "\$match" to mapOf("\$expr" to mapOf(
                "\$and" to listOf(
                    mapOf("\$ne" to listOf("\$flow.vhi", "\$flow.avhi"))
                )
            ))
        ))
        val group = D(mapOf(
            "\$group" to mapOf(
                "_id" to mapOf(
                    P.si to  "\$_id",
                    "ai" to "\$ai",
                    "av" to "\$av",
                    "bvhi" to "\$flow.bvhi",
                    "vhi" to "\$flow.vhi",
                    "avhi" to "\$flow.avhi",
                    P.si to mapOf(
                        "\$concat" to listOf(
                            mapOf("\$substr" to listOf(
                                mapOf("\$dateToString" to mapOf(
                                    "format" to "%Y-%m-%d %H:%M",
                                    "date" to "\$st"
                                )), 0, 15
                            )), "0:00"
                        )
                    )
                ),
                "mco" to mapOf("\$sum" to 1),
                "rco" to mapOf("\$sum" to 1)
            )
        ))
        val pipeline = listOf(
            match,
            lookup,
            addFields,
            unwind,
            match2,
            group
        )

        return Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
            pipeline
        ).allowDiskUse(false).explain()).collectList().map {
            println(Util.toPretty(it))
            Status.status200Ok("Ok") //Util.toPretty(it[0].toJson())
        }
    }
    /**
     * Heatmap Pipeline Test
     */
    fun test5(req: SafeRequest): Mono<Map<String, Any>> {
        val zdt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zdt.minusDays(30)
        val toDt = zdt.plusDays(30)

        val pipeline = listOf(
            D(mapOf("\$match" to mapOf(
                "\$or" to listOf(
                    mapOf(P.i to mapOf(
                        "\$gte" to fromDt.toInstant(),
                        "\$lt" to toDt.toInstant()
                    )),
                    mapOf("\$and" to listOf(
                        mapOf(P.i to mapOf("\$exists" to false)),
                        mapOf(P.st to mapOf(
                            "\$gte" to fromDt.minusDays(1).toInstant(),
                            "\$lt" to toDt.minusDays(1).toInstant()
                        ))
                    ))
                )
            ))),
            D(mapOf("\$lookup" to mapOf(
                "from" to "event",
                "let" to mapOf(P.si to "\$_id"),
                "pipeline" to listOf(
                    mapOf("\$match" to mapOf("\$expr" to mapOf(
                        "\$and" to listOf(
                            mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                            mapOf("\$eq" to listOf("\$t", ET.VIEW_START)),
                            mapOf("\$ne" to listOf("\$vw", 0)),
                            mapOf("\$ne" to listOf("\$vh", 0))
                        )
                    ))),
                    mapOf("\$group" to mapOf(
                        "_id" to mapOf(
                            "vhi" to "\$vhi",
                            "vo" to "\$vo",
                            "uts" to "\$uts"
                        )
                    ))
                ),
                "as" to "event"
            ))),
            D(mapOf("\$unwind" to "\$event")),
            D(mapOf("\$project" to mapOf(
                "_id" to 0,
                P.si to  "\$si",
                "ai" to "\$ai",
                "av" to "\$av",
                "dw" to "\$dw",
                "dh" to "\$dh",
                P.si to "\$st",
                "vhi" to "\$event._id.vhi",
                "vo" to "\$event._id.vo",
                "uts" to "\$event._id.uts"
            )))
        )
        return Flux
            .from(MongodbUtil.getCollection("session")
                .aggregate(pipeline).allowDiskUse(true))
            .collectList()
            .map {
//                println(Util.toPretty(it))
                Status.status200Ok(it.count())
            }
    }
    /**
     * Heatmap No $lookup Test
     */
    fun test7(req: SafeRequest): Mono<Map<String, Any>> {
        val zdt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zdt.minusDays(30)
        val toDt = zdt.plusDays(30)

        val pipeline = listOf(
            D(mapOf("\$match" to mapOf(
                "\$or" to listOf(
                    mapOf(P.i to mapOf(
                        "\$gte" to fromDt.toInstant(),
                        "\$lt" to toDt.toInstant()
                    )),
                    mapOf("\$and" to listOf(
                        mapOf(P.i to mapOf("\$exists" to false)),
                        mapOf(P.st to mapOf(
                            "\$gte" to fromDt.minusDays(1).toInstant(),
                            "\$lt" to toDt.minusDays(1).toInstant()
                        ))
                    ))
                ),
            ))),
            D(mapOf("\$match" to mapOf(
                "\$or" to listOf(
                    mapOf("\$and" to listOf(
                        mapOf("t" to mapOf("\$eq" to ET.VIEW_START)),
                        mapOf("vw" to mapOf("\$ne" to 0)),
                        mapOf("vh" to mapOf("\$ne" to 0)),
                    )),
                    mapOf("\$and" to listOf(
                        mapOf("t" to mapOf("\$in" to listOf(ET.REACT_TAP, ET.REACT_LONG_TAP, ET.REACT_SWIPE, ET.REACT_DOUBLE_TAP, ET.NOACT_TAP, ET.NOACT_DOUBLE_TAP, ET.NOACT_SWIPE, ET.NOACT_LONG_TAP))),
                        mapOf("gx" to mapOf("\$ne" to -1)),
                        mapOf("gy" to mapOf("\$ne" to -1)),
                    ))
                )
            ))),
            D(mapOf("\$group" to mapOf(
                "_id" to mapOf(
                    P.si to  "\$_id.si",
                    "ai" to "\$ai",
                    "av" to "\$av",
                    "dw" to "\$dw",
                    "dh" to "\$dh",
                    P.si to "\$st",
                    "uts" to "\$uts"
                ),
                "view" to mapOf(
                    "\$push" to mapOf(
                        "\$cond" to listOf(
                            mapOf("\$ne" to listOf("\$t", ET.VIEW_START)),
                            "\$\$REMOVE",
                            mapOf(
                                "vo" to "\$vo",
                                "vhi" to "\$vhi"
                            )
                        )
                    )
                ),
                "data" to mapOf(
                    "\$push" to mapOf(
                        "\$cond" to listOf(
                            mapOf("\$in" to listOf("\$t", listOf(ET.REACT_TAP, ET.REACT_LONG_TAP, ET.REACT_SWIPE, ET.REACT_DOUBLE_TAP, ET.NOACT_TAP, ET.NOACT_DOUBLE_TAP, ET.NOACT_SWIPE, ET.NOACT_LONG_TAP))),
                            mapOf(
                                "gx" to "\$gx", // Swipe
                                "gy" to "\$gy",  // Swipe
                                "gex" to "\$gex",
                                "gey" to "\$gey",
                                "t" to "\$t",
                                P.i to "\$i",
                                "ci" to "\$ci",
                            ),
                            "\$\$REMOVE"
                        )
                    )
                ),
            ))),
            D(mapOf("\$unwind" to mapOf(
                "path" to "\$data",
//                "preserveNullAndEmptyArrays" to true
            ))),
            D(mapOf("\$project" to mapOf(
                "_id" to 0,
                P.si to  "\$_id.si",
                "ai" to "\$_id.ai",
                "av" to "\$_id.av",
                "dw" to "\$_id.dw",
                "dh" to "\$_id.dh",
                P.si to "\$_id.st",
                "uts" to "\$_id.uts",
                "t" to "\$data.t",
                P.i to "\$data.i",
                "ci" to "\$data.ci",
                "vo" to mapOf("\$arrayElemAt" to listOf("\$view.vo", 0)),
                "vhi" to mapOf("\$ifNull" to listOf(mapOf("\$arrayElemAt" to listOf("\$view.vhi", 0)), false)),
//                "vhi" to mapOf("\$arrayElemAt" to listOf("\$view.vhi", 0)),
                "gx" to "\$data.gx",
                "gy" to "\$data.gy",
                "gex" to "\$data.gex",
                "gey" to "\$data.gey"
            ))),
            D(mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                mapOf("\$lte" to listOf("\$gx", mapOf("\$cond" to listOf(mapOf("\$eq" to listOf("\$vo", 1)), "\$dw", "\$dh")))),
                mapOf("\$lte" to listOf("\$gy", mapOf("\$cond" to listOf(mapOf("\$eq" to listOf("\$vo", 1)), "\$dh", "\$dw")))),
                mapOf("\$ne" to listOf("\$vhi", false))
            ))))),
            D(mapOf("\$group" to mapOf(
                "_id" to mapOf(
                    "ai" to "\$ai",
                    "av" to "\$av",
                    "vhi" to "\$vhi",
                    "t" to "\$t",
                    "vo" to "\$vo",
                    P.si to mapOf("\$toDate" to mapOf(
                        "\$concat" to listOf(
                            mapOf("\$substr" to listOf(
                                mapOf("\$dateToString" to mapOf(
                                    "format" to "%Y-%m-%d %H:%M",
                                    "date" to "\$st"
                                )), 0, 15)), "0:00"
                        )
                    )),
                    "hx" to mapOf("\$round" to listOf(mapOf("\$cond" to listOf(
                        mapOf("\$eq" to listOf("\$vo", 1)),
                        mapOf("\$divide" to listOf("\$gx", "\$dw")),
                        mapOf("\$divide" to listOf("\$gx", "\$dh"))
                    )), 2)),
                    "hy" to mapOf("\$round" to listOf(mapOf("\$cond" to listOf(
                        mapOf("\$eq" to listOf("\$vo", 1)),
                        mapOf("\$divide" to listOf("\$gy", "\$dh")),
                        mapOf("\$divide" to listOf("\$gy", "\$dw"))
                    )), 2)),
                    "hex" to mapOf("\$round" to listOf(mapOf("\$cond" to listOf(
                        mapOf("\$eq" to listOf("\$vo", 1)),
                        mapOf("\$divide" to listOf("\$gex", "\$dw")),
                        mapOf("\$divide" to listOf("\$gex", "\$dh"))
                    )), 2)),
                    "hey" to mapOf("\$round" to listOf(mapOf("\$cond" to listOf(
                        mapOf("\$eq" to listOf("\$vo", 1)),
                        mapOf("\$divide" to listOf("\$gey", "\$dh")),
                        mapOf("\$divide" to listOf("\$gey", "\$dw"))
                    )), 2))
                ),
                "count" to mapOf("\$sum" to 1)
            ))),
            D(mapOf("\$lookup" to mapOf(
                "from" to "app",
                "localField" to "_id.ai",
                "foreignField" to "_id",
                "as" to "app"
            ))),
            D(mapOf("\$replaceWith" to mapOf("\$mergeObjects" to listOf(mapOf(
                "ai" to "\$_id.ai",
                "av" to "\$_id.av",
                P.si to "\$_id.st",
                "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                    "date" to "\$_id.st",
                    "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                ))),
                "vhi" to "\$_id.vhi",
                "hx" to "\$_id.hx",
                "hy" to "\$_id.hy",
                "hex" to mapOf("\$ifNull" to listOf("\$_id.hex", 0)),
                "hey" to mapOf("\$ifNull" to listOf("\$_id.hey", 0)),
                "t" to "\$_id.t",
                "vo" to "\$_id.vo",
                "bft" to mapOf("\$toDate" to fromDt.toInstant()),
                "count" to "\$count"
            ))))),
            D(mapOf("\$merge" to mapOf(
                "into" to IndicatorHeatmapByView.COLLECTION_NAME_PT10M,
                "on" to listOf("stz", "st", "ai", "av", "vhi", "t", "vo", "hx", "hy", "hex", "hey"),
                "whenMatched" to listOf(
                    mapOf("\$addFields" to mapOf(
                        "stz" to "\$\$new.stz",
                        P.si to "\$st",
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "vhi" to "\$vhi",
                        "t" to "\$t",
                        "vo" to "\$vo",
                        "hx" to "\$hx",
                        "hy" to "\$hy",
                        "hex" to "\$hex",
                        "hey" to "\$hey",
                        "bft" to "\$\$new.bft",
                        "count" to mapOf("\$sum" to listOf("\$count", "\$\$new.count"))
                    ))
                ),
                "whenNotMatched" to "insert"
            )))
        )

        return Flux
            .from(MongodbUtil.getCollection(EventService.COLLECTION_NAME)
                .aggregate(pipeline).allowDiskUse(true)
//                .explain(ExplainVerbosity.EXECUTION_STATS)
            )
            .collectList()
            .map {
//                println(it.first())
//                println(it.last())
                Status.status200Ok(it.count())
            }
    }
    fun test_ (req: SafeRequest): Mono<Map<String, Any>> {
        val str = """
[
    {"message": 
        {"#and": [
            {
                "test" : 123
            }   ]
        }
    }
]
        """.trimIndent()
        val str2 = listOf(mapOf("message" to mapOf(
            "\$and" to listOf(
                mapOf("test" to 123)
            )
        )))
        println("$str\n$str2")
        return Mono.just(mapOf("str 1" to str, "str 2" to Util.toPretty(str2), "toJson" to Util.toJsonString(Util.jsonToList(Util.toPretty(str2)))))
    }

    fun heatmapByViewBatch(req: SafeRequest): Mono<Map<String, Any>> {
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val orientation = req.getQueryOrDefault("orientation", "1")
        val by = req.getQueryOrDefault("by", "view")
        val type = req.getQueryOrDefault("type", "tap")

        val validator = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
        if (validator.isNotValid()) throw PolarisException.status400BadRequest(validator.toExceptionList())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        return AppService.getAppIdList(Protocol.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val dateQry = mapOf("\$or" to listOf(
                    mapOf(P.i to mapOf(
                        "\$gte" to from,
                        "\$lt" to to)),
                    mapOf("\$and" to listOf(
                        mapOf(P.i to mapOf("\$exists" to false)),
                        mapOf(P.st to mapOf(
                            "\$gte" to from.minusDays(1),
                            "\$lt" to to.minusDays(1)))
                    ))
                ))
                val joinToStartViewFromEventQry = mapOf(
                    "from" to "event",
                    "let" to mapOf(P.si to "\$_id"),
                    "pipeline" to listOf(
                        mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                            mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                            mapOf("\$eq" to listOf("\$t", ET.VIEW_START)),
                            mapOf("\$ne" to listOf("\$vw", 0)),
                            mapOf("\$ne" to listOf("\$vh", 0))
                        )))),
                        mapOf("\$group" to mapOf(
                            "_id" to mapOf(
                                "vhi" to "\$vhi",
                                "vo" to "\$vo",
                                "uts" to "\$uts"
                            )
                        ))
                    ),
                    "as" to "event"
                )
                val projectQry = mapOf(
                    "_id" to 0,
                    P.si to  "\$_id",
                    "ai" to "\$ai",
                    "av" to "\$av",
                    "dw" to "\$dw",
                    "dh" to "\$dh",
                    P.si to "\$st",
                    "vhi" to "\$event._id.vhi",
                    "vo" to "\$event._id.vo",
                    "uts" to "\$event._id.uts"
                )
                val joinFromEventQry = mapOf(
                    "from" to "event",
                    "let" to mapOf(
                        P.si to  "\$si",
                        "vhi" to "\$vhi",
                        "uts" to "\$uts",
                        "dw" to "\$dw",
                        "dh" to "\$dh",
                        "vo" to "\$vo",
                    ),
                    "pipeline" to listOf(
                        mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                            mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
//							mapOf("\$eq" to listOf("\$vhi", "\$\$vhi")), // Todo: 로직 체크 필요 Git issue 참조
                            mapOf("\$eq" to listOf("\$uts", "\$\$uts")),
                            mapOf("\$in" to listOf("\$t", listOf(ET.REACT_TAP, ET.REACT_DOUBLE_TAP, ET.REACT_LONG_TAP, ET.REACT_SWIPE, ET.NOACT_TAP, ET.NOACT_DOUBLE_TAP, ET.NOACT_LONG_TAP, ET.NOACT_SWIPE))),
                            mapOf("\$ne" to listOf("\$gx", -1)),
                            mapOf("\$ne" to listOf("\$gy", -1)),
                            mapOf("\$lte" to listOf("\$gx", mapOf("\$cond" to listOf(mapOf("\$eq" to listOf("\$\$vo", 1)), "\$\$dw", "\$\$dh")))),
                            mapOf("\$lte" to listOf("\$gy", mapOf("\$cond" to listOf(mapOf("\$eq" to listOf("\$\$vo", 1)), "\$\$dh", "\$\$dw"))))
                        ))))
                    ),
                    "as" to "event"
                )
                val groupQry = mapOf(
                    "_id" to mapOf(
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "vhi" to "\$vhi",
                        "hx" to mapOf("\$round" to listOf(
                            mapOf("\$cond" to listOf(
                                mapOf("\$eq" to listOf("\$vo", 1)),
                                mapOf("\$divide" to listOf("\$event.gx", "\$dw")),
                                mapOf("\$divide" to listOf("\$event.gx", "\$dh"))
                            )), 2
                        )),
                        "hy" to mapOf("\$round" to listOf(
                            mapOf("\$cond" to listOf(
                                mapOf("\$eq" to listOf("\$vo", 1)),
                                mapOf("\$divide" to listOf("\$event.gy", "\$dh")),
                                mapOf("\$divide" to listOf("\$event.gy", "\$dw"))
                            )), 2
                        )),
                        "hex" to mapOf("\$round" to listOf(
                            mapOf("\$cond" to listOf(
                                mapOf("\$eq" to listOf("\$vo", 1)),
                                mapOf("\$divide" to listOf("\$event.gex", "\$dw")),
                                mapOf("\$divide" to listOf("\$event.gex", "\$dh"))
                            )), 2
                        )),
                        "hey" to mapOf("\$round" to listOf(
                            mapOf("\$cond" to listOf(
                                mapOf("\$eq" to listOf("\$vo", 1)),
                                mapOf("\$divide" to listOf("\$event.gey", "\$dh")),
                                mapOf("\$divide" to listOf("\$event.gey", "\$dw"))
                            )), 2
                        )),
                        "t" to "\$event.t",
                        "vo" to "\$vo",
                        P.si to mapOf("\$toDate" to mapOf("\$concat" to listOf(
                            mapOf("\$substr" to listOf(
                                mapOf("\$dateToString" to mapOf("format" to "%Y-%m-%d %H:%M", "date" to "\$st")),
                                0, 15
                            )),
                            "0:00"
                        )))
                    ),
                    "count" to mapOf("\$sum" to 1)
                )
                val joinFromAppQry = mapOf(
                    "from" to "app",
                    "localField" to "_id.ai",
                    "foreignField" to "_id",
                    "as" to "app"
                )
                val replaceQry = mapOf(
                    "\$mergeObjects" to listOf(
                        mapOf(
                            "ai" to "\$_id.ai",
                            "av" to "\$_id.av",
                            P.si to "\$_id.st",
                            "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                                "date" to "\$_id.st",
                                "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                            ))),
                            "vhi" to "\$_id.vhi",
                            "hx" to "\$_id.hx",
                            "hy" to "\$_id.hy",
                            "hex" to mapOf("\$ifNull" to listOf("\$_id.hex", 0)),
                            "hey" to mapOf("\$ifNull" to listOf("\$_id.hey", 0)),
                            "t" to "\$_id.t",
                            "vo" to "\$_id.vo",
                            "bft" to mapOf("\$toDate" to from),
                            "count" to "\$count"
                        )
                    )
                )
                val mergeQry = mapOf(
                    "into" to IndicatorHeatmapByView.COLLECTION_NAME_PT10M+"_old",
                    "on" to listOf("stz", "st", "ai", "av", "vhi", "t", "vo", "hx", "hy", "hex", "hey"),
                    "whenMatched" to listOf(
                        mapOf("\$addFields" to mapOf(
                            "stz" to "\$\$new.stz",
                            P.si to "\$st",
                            "ai" to "\$ai",
                            "av" to "\$av",
                            "vhi" to "\$vhi",
                            "t" to "\$t",
                            "vo" to "\$vo",
                            "hx" to "\$hx",
                            "hy" to "\$hy",
                            "hex" to "\$hex",
                            "hey" to "\$hey",
                            "bft" to "\$\$new.bft",
                            "count" to mapOf("\$sum" to listOf("\$count", "\$\$new.count"))
                        ))),
                    "whenNotMatched" to "insert"
                )

                val pipeline = listOf(
                    D().append("\$match", dateQry),
                    D().append("\$lookup", joinToStartViewFromEventQry),
                    D().append("\$unwind", "\$event"),
                    D().append("\$project", projectQry),
                    D().append("\$lookup", joinFromEventQry),
                    D().append("\$unwind", "\$event"),
                    D().append("\$group", groupQry),
                    D().append("\$lookup", joinFromAppQry),
                    D().append("\$replaceWith", replaceQry),
                    D().append("\$merge", mergeQry),
                )

//                println(pipeline)
                Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
                    pipeline
                ).allowDiskUse(true)
//                    .explain(ExplainVerbosity.EXECUTION_STATS)
                )
                    .collectList()
                    .map {
                        println(it.first())
                        println(it.last())
//                        println(it)
//                        println(Util.toPretty(it))
                        Status.status200Ok(it.count())
                    }
            }
    }
    /**
     * Heatmap Performance Function
     */
    fun new_heatmap(req: SafeRequest) : Mono<Map<String, Any>> {
        val zdt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zdt.minusDays(360)
        val toDt = zdt.plusDays(360)

        val pipeline = listOf(
            D(mapOf("\$match" to mapOf(
                "\$or" to listOf(
                    mapOf(P.i to mapOf(
                        "\$gte" to fromDt.toInstant(),
                        "\$lt" to toDt.toInstant())),
                    mapOf("\$and" to listOf(
                        mapOf(P.i to mapOf("\$exists" to false)),
                        mapOf(P.st to mapOf(
                            "\$gte" to fromDt.minusDays(1).toInstant(),
                            "\$lt" to toDt.minusDays(1).toInstant()))
                    ))
                )
            ))),
            D(mapOf("\$lookup" to mapOf(
                "from" to "event",
                "let" to mapOf(P.si to "\$_id"),
                "pipeline" to listOf(
                    mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                        mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                        mapOf("\$eq" to listOf("\$t", ET.VIEW_START)),
                        mapOf("\$ne" to listOf("\$vw", 0)),
                        mapOf("\$ne" to listOf("\$vh", 0))
                    )))),
                    mapOf("\$group" to mapOf(
                        "_id" to mapOf(
                            "vhi" to "\$vhi",
                            "vo" to "\$vo",
                            "uts" to "\$uts"
                        )
                    ))
                ),
                "as" to "view"
            ))),
            D(mapOf("\$unwind" to "\$view")),
            D(mapOf("\$lookup" to mapOf(
                "from" to "event",
                "localField" to "_id",
                "foreignField" to "_id.si",
                "as" to "event"
            ))),
            D(mapOf("\$project" to mapOf(
                "_id" to 0,
                P.si to  "\$_id",
                "ai" to "\$ai",
                "av" to "\$av",
                "dw" to "\$dw",
                "dh" to "\$dh",
                P.si to "\$st",
                "vhi" to "\$view._id.vhi",
                "vo" to "\$view._id.vo",
                "uts" to "\$view._id.uts",
                "event" to mapOf("\$filter" to mapOf(
                    "input" to "\$event",
                    "as" to "event",
                    "cond" to mapOf("\$and" to listOf(
                        mapOf("\$in" to listOf("\$\$event.t", listOf(ET.REACT_TAP, ET.REACT_DOUBLE_TAP, ET.REACT_LONG_TAP, ET.REACT_SWIPE, ET.NOACT_TAP, ET.NOACT_DOUBLE_TAP, ET.NOACT_LONG_TAP, ET.NOACT_SWIPE))),
                        mapOf("\$eq" to listOf("\$\$event.uts", "\$view._id.uts")),
                        mapOf("\$ne" to listOf("\$\$event.gx", -1)),
                        mapOf("\$ne" to listOf("\$\$event.gy", -1)),
                        mapOf("\$lte" to listOf("\$\$event.gx", mapOf("\$cond" to listOf(mapOf("\$eq" to listOf("\$view._id.vo", 1)), "\$dw", "\$dh")))),
                        mapOf("\$lte" to listOf("\$\$event.gy", mapOf("\$cond" to listOf(mapOf("\$eq" to listOf("\$view._id.vo", 1)), "\$dh", "\$dw"))))
                    ))
                ))
            ))),
//            D(mapOf("\$match" to mapOf(
//                "event" to mapOf("\$elemMatch" to mapOf("\$exists" to true))
//            ))),
            D(mapOf("\$unwind" to "\$event")),
            D(mapOf("\$group" to mapOf(
                "_id" to mapOf(
                    "ai" to "\$ai",
                    "av" to "\$av",
                    "vhi" to "\$vhi",
                    "hx" to mapOf("\$round" to listOf(
                        mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf("\$vo", 1)),
                            mapOf("\$divide" to listOf("\$event.gx", "\$dw")),
                            mapOf("\$divide" to listOf("\$event.gx", "\$dh"))
                        )), 2
                    )),
                    "hy" to mapOf("\$round" to listOf(
                        mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf("\$vo", 1)),
                            mapOf("\$divide" to listOf("\$event.gy", "\$dh")),
                            mapOf("\$divide" to listOf("\$event.gy", "\$dw"))
                        )), 2
                    )),
                    "hex" to mapOf("\$round" to listOf(
                        mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf("\$vo", 1)),
                            mapOf("\$divide" to listOf("\$event.gex", "\$dw")),
                            mapOf("\$divide" to listOf("\$event.gex", "\$dh"))
                        )), 2
                    )),
                    "hey" to mapOf("\$round" to listOf(
                        mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf("\$vo", 1)),
                            mapOf("\$divide" to listOf("\$event.gey", "\$dh")),
                            mapOf("\$divide" to listOf("\$event.gey", "\$dw"))
                        )), 2
                    )),
                    "t" to "\$event.t",
                    "vo" to "\$vo",
                    P.si to mapOf("\$toDate" to mapOf("\$concat" to listOf(
                        mapOf("\$substr" to listOf(
                            mapOf("\$dateToString" to mapOf("format" to "%Y-%m-%d %H:%M", "date" to "\$st")),
                            0, 15
                        )),
                        "0:00"
                    )))
                ),
                "count" to mapOf("\$sum" to 1)
            ))),
            D(mapOf("\$lookup" to mapOf(
                "from" to "app",
                "localField" to "_id.ai",
                "foreignField" to "_id",
                "as" to "app"
            ))),
            D(mapOf("\$replaceWith" to mapOf(
                "\$mergeObjects" to listOf(
                    mapOf(
                        "ai" to "\$_id.ai",
                        "av" to "\$_id.av",
                        P.si to "\$_id.st",
                        "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                            "date" to "\$_id.st",
                            "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                        ))),
                        "vhi" to "\$_id.vhi",
                        "hx" to "\$_id.hx",
                        "hy" to "\$_id.hy",
                        "hex" to mapOf("\$ifNull" to listOf("\$_id.hex", 0)),
                        "hey" to mapOf("\$ifNull" to listOf("\$_id.hey", 0)),
                        "t" to "\$_id.t",
                        "vo" to "\$_id.vo",
                        "bft" to mapOf("\$toDate" to fromDt.toInstant()),
                        "count" to "\$count"
                    )
                )
            ))),
            D(mapOf("\$merge" to mapOf(
                "into" to IndicatorHeatmapByView.COLLECTION_NAME_PT10M+"_new",
                "on" to listOf("stz", "st", "ai", "av", "vhi", "t", "vo", "hx", "hy", "hex", "hey"),
                "whenMatched" to listOf(
                    mapOf("\$addFields" to mapOf(
                        "stz" to "\$\$new.stz",
                        P.si to "\$st",
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "vhi" to "\$vhi",
                        "t" to "\$t",
                        "vo" to "\$vo",
                        "hx" to "\$hx",
                        "hy" to "\$hy",
                        "hex" to "\$hex",
                        "hey" to "\$hey",
                        "bft" to "\$\$new.bft",
                        "count" to mapOf("\$sum" to listOf("\$count", "\$\$new.count"))
                    ))),
                "whenNotMatched" to "insert"
            )))
        )
        println(pipeline)
        return Flux
            .from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME)
                .aggregate(pipeline)
                .allowDiskUse(true)
//                .explain(ExplainVerbosity.EXECUTION_STATS)
            )
            .collectList()
            .map {
                println(it.first())
                println(it.last())
//                println(it)
//                println(Util.toPretty(it))
                Status.status200Ok(it.count())
            }
    }

    /**
     *  View List Batch Test
     */
    fun viewListOneDaybatch(req: SafeRequest): Mono<Map<String, Any>> {
        val zdt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zdt.minusDays(3)
        val toDt = zdt.plusDays(3)
        val s3 = Config.get("s3.region")
        val fullPath = QueryPeasant.getImageURI()

        val pipeline = listOf(
            D(mapOf("\$match" to mapOf(
                "bft" to mapOf(
                    "\$gte" to fromDt.toInstant(),
                    "\$lt" to toDt.toInstant()
                    )
                )
            )),
            D(mapOf("\$group" to mapOf(
                "_id" to mapOf(
                    "stzd" to "\$stzd",
                    "ai" to "\$ai",
                    "av" to "\$av",
                    "vhi" to "\$vhi"
                )
            ))),
            D(mapOf("\$replaceWith" to mapOf("\$mergeObjects" to listOf(mapOf(
                "stzd" to "\$_id.stzd",
                "ai" to "\$_id.ai",
                "av" to "\$_id.av",
                "vhi" to "\$_id.vhi"
            ))))),
            D(mapOf("\$lookup" to mapOf(
                "from" to ViewList.COLLECTION_NAME_PT10M,
                "let" to mapOf("stzd" to "\$stzd", "ai" to "\$ai", "av" to "\$av", "vhi" to "\$vhi"),
                "pipeline" to listOf(mapOf(
                    "\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                        mapOf("\$eq" to listOf("\$stzd", "\$\$stzd")),
                        mapOf("\$eq" to listOf("\$ai", "\$\$ai")),
                        mapOf("\$eq" to listOf("\$av", "\$\$av")),
                        mapOf("\$eq" to listOf("\$vhi", "\$\$vhi"))
                    )))
                )),
                "as" to "data"
            ))),
            D(mapOf("\$unwind" to "\$data")),
            D(mapOf("\$group" to mapOf(
                "_id" to mapOf(
                    "ai" to "\$data.ai",
                    "av" to "\$data.av",
                    "vi" to "\$data.vi",
                    "vhi" to "\$data.vhi",
                    "stz" to "\$data.stzd"
                )
            ))),
            D(mapOf("\$lookup" to mapOf(
                "from" to StorageService.COLLECTION_NAME,
                "let" to mapOf("ai" to "\$_id.ai", "av" to "\$_id.av", "vi" to "\$_id.vi"),
                "pipeline" to listOf(
                    mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                        mapOf("\$eq" to listOf("\$ai", "\$\$ai")),
                        mapOf("\$eq" to listOf("\$av", "\$\$av")),
                        mapOf("\$eq" to listOf("\$vi", "\$\$vi"))
                    ))))
                ),
                "as" to "storage"
            ))),
            D(mapOf("\$unwind" to mapOf(
                "path" to "\$storage",
                "preserveNullAndEmptyArrays" to true
            ))),
            /**
             * S3 URL: s3://service-uh-polaris-stage-2/attach/b3068e50a8afca37a2909990f9b8c0f7efbe2168/1.0.1/-1044417443
             * 객체 URl: https://service-uh-polaris-stage-2.s3.ap-northeast-2.amazonaws.com/attach/b3068e50a8afca37a2909990f9b8c0f7efbe2168/1.0.1/-1044417443
             * GET storage: path": "s3:/b3068e50a8afca37a2909990f9b8c0f7efbe2168/1.0.1/530167440
             */
            D(mapOf("\$replaceWith" to mapOf(
                "\$mergeObjects" to listOf(mapOf(
                    "ai" to "\$_id.ai",
                    "av" to "\$_id.av",
                    "vi" to "\$_id.vi",
                    "vhi" to "\$_id.vhi",
                    "path" to fullPath
                ))
            ))),
            D(mapOf("\$sort" to mapOf("av" to 1))),
            D(mapOf("\$merge" to mapOf(
                "into" to ViewList.COLLECTION_NAME_PT24H,
                "on" to listOf("ai", "av", "vhi"),
                "whenMatched" to "keepExisting",
                "whenNotMatched" to "insert"
            )))
        )

            return Flux.from(MongodbUtil.getCollection(ViewList.COLLECTION_NAME_PT10M)
                .aggregate(pipeline).allowDiskUse(true)
//                .explain(ExplainVerbosity.EXECUTION_STATS)
            )
                .collectList()
                .map {
//                    println(it.first())
//                    println(it.last())
                    println(Util.toPretty(it))
                    Status.status200Ok(it) }
    }
    /*
    현석님 요청 히트맵 배치 테스트
     */
    fun heatmapTest(req: SafeRequest): Mono<Map<String,Any>> {
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val validator = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
        if (validator.isNotValid()) throw PolarisException.status400BadRequest(validator.toExceptionList())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        val pipeline = listOf(
            D(mapOf("\$match" to mapOf(
                "\$or" to listOf(
                    mapOf(P.i to mapOf(
                        "\$gte" to from,
                        "\$lt" to to)),
                    mapOf("\$and" to listOf(
                        mapOf(P.i to mapOf("\$exists" to false)),
                        mapOf(P.st to mapOf(
                            "\$gte" to from.minusDays(1),
                            "\$lt" to to.minusDays(1)))
                    ))
                )
            ))),
            D(mapOf("\$lookup" to mapOf(
                "from" to "event",
                "let" to mapOf(P.si to "\$_id"),
                "pipeline" to listOf(
                    mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                        mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                        mapOf("\$eq" to listOf("\$t", ET.VIEW_START)),
                        mapOf("\$ne" to listOf("\$vw", 0)),
                        mapOf("\$ne" to listOf("\$vh", 0))
                    )))),
                    mapOf("\$group" to mapOf(
                        "_id" to mapOf(
                            "vhi" to "\$vhi",
                            "vo" to "\$vo",
                            "uts" to "\$uts"
                        )
                    ))
                ),
                "as" to "view"
            ))),
            D(mapOf("\$unwind" to "\$view")),
            D(mapOf("\$lookup" to mapOf(
                "from" to "event",
                "localField" to "_id",
                "foreignField" to "_id.si",
                "as" to "event"
            ))),
            D(mapOf("\$project" to mapOf(
                "_id" to 0,
                P.si to  "\$_id",
                "ai" to "\$ai",
                "av" to "\$av",
                "dw" to "\$dw",
                "dh" to "\$dh",
                P.si to "\$st",
                "vhi" to "\$view._id.vhi",
                "vo" to "\$view._id.vo",
                "uts" to "\$view._id.uts",
                "event" to mapOf("\$filter" to mapOf(
                    "input" to "\$event",
                    "as" to "event",
                    "cond" to mapOf("\$and" to listOf(
                        mapOf("\$in" to listOf("\$\$event.t", listOf(ET.REACT_TAP, ET.REACT_DOUBLE_TAP, ET.REACT_LONG_TAP, ET.REACT_SWIPE, ET.NOACT_TAP, ET.NOACT_DOUBLE_TAP, ET.NOACT_LONG_TAP, ET.NOACT_SWIPE))),
                        mapOf("\$eq" to listOf("\$\$event.uts", "\$view._id.uts")),
                        mapOf("\$ne" to listOf("\$\$event.gx", -1)),
                        mapOf("\$ne" to listOf("\$\$event.gy", -1)),
                        mapOf("\$lte" to listOf("\$\$event.gx", mapOf("\$cond" to listOf(mapOf("\$eq" to listOf("\$view._id.vo", 1)), "\$dw", "\$dh")))),
                        mapOf("\$lte" to listOf("\$\$event.gy", mapOf("\$cond" to listOf(mapOf("\$eq" to listOf("\$view._id.vo", 1)), "\$dh", "\$dw"))))
                    ))
                ))
            ))),
//            D(mapOf("\$match" to mapOf(
//                "event" to mapOf("\$elemMatch" to mapOf("\$exists" to true))
//            ))),
            D(mapOf("\$unwind" to "\$event")),
            D(mapOf("\$group" to mapOf(
                "_id" to mapOf(
                    "ai" to "\$ai",
                    "av" to "\$av",
                    "vhi" to "\$vhi",
                    "hx" to mapOf("\$round" to listOf(
                        mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf("\$vo", 1)),
                            mapOf("\$divide" to listOf("\$event.gx", "\$dw")),
                            mapOf("\$divide" to listOf("\$event.gx", "\$dh"))
                        )), 2
                    )),
                    "hy" to mapOf("\$round" to listOf(
                        mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf("\$vo", 1)),
                            mapOf("\$divide" to listOf("\$event.gy", "\$dh")),
                            mapOf("\$divide" to listOf("\$event.gy", "\$dw"))
                        )), 2
                    )),
                    "hex" to mapOf("\$round" to listOf(
                        mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf("\$vo", 1)),
                            mapOf("\$divide" to listOf("\$event.gex", "\$dw")),
                            mapOf("\$divide" to listOf("\$event.gex", "\$dh"))
                        )), 2
                    )),
                    "hey" to mapOf("\$round" to listOf(
                        mapOf("\$cond" to listOf(
                            mapOf("\$eq" to listOf("\$vo", 1)),
                            mapOf("\$divide" to listOf("\$event.gey", "\$dh")),
                            mapOf("\$divide" to listOf("\$event.gey", "\$dw"))
                        )), 2
                    )),
                    "t" to "\$event.t",
                    "vo" to "\$vo",
                    P.si to mapOf("\$toDate" to mapOf("\$concat" to listOf(
                        mapOf("\$substr" to listOf(
                            mapOf("\$dateToString" to mapOf("format" to "%Y-%m-%d %H:%M", "date" to "\$st")),
                            0, 15
                        )),
                        "0:00"
                    )))
                ),
                "count" to mapOf("\$sum" to 1)
            ))),
            D(mapOf("\$lookup" to mapOf(
                "from" to "app",
                "localField" to "_id.ai",
                "foreignField" to "_id",
                "as" to "app"
            ))),
            D(mapOf("\$replaceWith" to mapOf(
                "\$mergeObjects" to listOf(
                    mapOf(
                        "ai" to "\$_id.ai",
                        "av" to "\$_id.av",
                        P.si to "\$_id.st",
                        "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                            "date" to "\$_id.st",
                            "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                        ))),
                        "vhi" to "\$_id.vhi",
                        "hx" to "\$_id.hx",
                        "hy" to "\$_id.hy",
                        "hex" to mapOf("\$ifNull" to listOf("\$_id.hex", 0)),
                        "hey" to mapOf("\$ifNull" to listOf("\$_id.hey", 0)),
                        "t" to "\$_id.t",
                        "vo" to "\$_id.vo",
                        "bft" to mapOf("\$toDate" to from),
                        "count" to "\$count"
                    )
                )
            ))),
            D(mapOf("\$merge" to mapOf(
                "into" to IndicatorHeatmapByView.COLLECTION_NAME_PT10M,
                "on" to listOf("stz", "st", "ai", "av", "vhi", "t", "vo", "hx", "hy", "hex", "hey"),
                "whenMatched" to listOf(
                    mapOf("\$addFields" to mapOf(
                        "stz" to "\$\$new.stz",
                        P.si to "\$st",
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "vhi" to "\$vhi",
                        "t" to "\$t",
                        "vo" to "\$vo",
                        "hx" to "\$hx",
                        "hy" to "\$hy",
                        "hex" to "\$hex",
                        "hey" to "\$hey",
                        "bft" to "\$\$new.bft",
                        "count" to mapOf("\$sum" to listOf("\$count", "\$\$new.count"))
                    ))),
                "whenNotMatched" to "insert"
            )))
        )
//        println(pipeline)
        return Flux
            .from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME)
                .aggregate(pipeline)
                .allowDiskUse(true)
//                .explain(ExplainVerbosity.EXECUTION_STATS)
            )
            .collectList()
            .map {
//                println(it.first())
//                println(it.last())
//                println(it)
                println(Util.toPretty(it))
                Status.status200Ok(it.count())
            }
    }

    fun flowServiceGetTarget(req: SafeRequest): Mono<Map<String, Any>> {
        val zdt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zdt.minusDays(60)
        val toDt = zdt.plusDays(1)

        return Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
            listOf(
                D(mapOf("\$match" to mapOf(
                    "st" to mapOf(
                        "\$gte" to fromDt.toInstant(),
                        "\$lt" to toDt.toInstant()
                    )
                ))),
                D("\$lookup", D()
                    .append("from", "event")
                    .append("localField", "_id")
                    .append("foreignField", "_id.${P.si}")
                    .append("as", "event")
                ),
                D(mapOf("\$project" to mapOf(
                    "_id" to 0,
                    "si" to "\$_id",
                    "ai" to "\$ai",
                    "av" to "\$av",

                ))),
                D(mapOf("\$lookup" to mapOf(
                    "from" to "event",
                    "let" to mapOf("si" to "\$_id"),
                    "pipeline" to listOf(mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                        mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                        mapOf("\$not" to mapOf("\$in" to listOf("\$t", listOf(ET.VIEW_START, ET.VIEW_END, ET.APP_END, ET.APP_START))))
                    ))))),
                    "as" to "event"
                ))),
                D("\$unwind", "\$event")
            )
        )).collectList().map { println(">> ${Util.toPretty(it)}"); Status.status200Ok(it) }
    }

    fun reach_rate (req:SafeRequest): Mono<Map<String, Any>> {
        val zt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zt.minusDays(30)
        val toDt = zt.plusDays(1)
        println("Date >> ${Date.from(fromDt.toInstant())}, ${Date.from(toDt.toInstant())}")
        return Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
            listOf(
                D("\$match", mapOf(
                    P.i to mapOf(
                        "\$gte" to Date.from(fromDt.toInstant()),
                        "\$lt" to Date.from(toDt.toInstant())
                    )
                )),
                D("\$lookup", mapOf(
                    "from" to "event",
                    "let" to mapOf("si" to "\$_id"),
                    "pipeline" to listOf(mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                        mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                        mapOf("\$eq" to listOf("\$t", ET.SCROLL_CHANGE)),
                        mapOf("\$gte" to listOf("\$spy", 300)),
                        mapOf("\$gt" to listOf("\$svi", null)),
                        mapOf("\$ne" to listOf("\$spx", -1)),
                        mapOf("\$ne" to listOf("\$spy", -1))
                    ))))),
                    "as" to "event"
                )),
                D("\$unwind", "\$event"),
                D("\$group", mapOf(
                    "_id" to mapOf(
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "vhi" to "\$event.ofvhi",
                        "svi" to "\$event.svi",
                        "spx" to mapOf("\$multiply" to listOf(
                            mapOf("\$toInt" to mapOf("\$divide" to listOf("\$event.spx", 300))),
                            300
                        )),
                        "spy" to mapOf("\$multiply" to listOf(
                            mapOf("\$toInt" to mapOf("\$divide" to listOf("\$event.spy", 300))),
                            300
                        )),
                        P.st to mapOf("\$toDate" to mapOf("\$concat" to listOf(
                            mapOf("\$substr" to listOf(
                                mapOf("\$dateToString" to mapOf(
                                    "format" to "%Y-%m-%d %H:%M",
                                    "date" to "\$st"
                                )), 0, 15
                            )),
                            "0:00"
                        )))
                    ),
                    "count" to mapOf("\$sum" to 1)
                )),
                D("\$addFields", mapOf(
                    "_id.ai" to mapOf(
                        "\$toObjectId" to "\$_id.ai"
                    )
                )),
                D("\$lookup", mapOf(
                    "from" to "app",
                    "localField" to "_id.ai",
                    "foreignField" to "_id",
                    "as" to "app"
                )),
                D("\$replaceWith", mapOf("\$mergeObjects" to listOf(mapOf(
                    "ai" to "\$_id.ai",
                    "av" to "\$_id.av",
                    "st" to "\$_id.st",
                    "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                        "date" to "\$_id.st",
                        "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                    ))),
                    "stzd" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                        "format" to "%Y-%m-%d 00:00:00",
                        "date" to "\$_id.st",
                        "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                    ))),
                    "vhi" to "\$_id.vhi",
                    "svi" to "\$_id.svi",
                    "spx" to "\$_id.spx",
                    "spy" to "\$_id.spy",
                    "bft" to mapOf("\$toDate" to Date.from(fromDt.toInstant())),
                    "count" to "\$count"
                )))),
//                D("\$merge", mapOf(
//                    ""
//                ))
            )
        )).collectList().map { println(">> ${Util.toPretty(it)}"); Status.status200Ok(it) }
    }
    fun scroll_view (req:SafeRequest): Mono<Map<String, Any>> {
        val zt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zt.minusDays(30)
        val toDt = zt.plusDays(1)
        println("Date >> ${Date.from(fromDt.toInstant())}, ${Date.from(toDt.toInstant())}")
        return Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
            listOf(
                D("\$match", mapOf(
                    P.i to mapOf(
                        "\$gte" to Date.from(fromDt.toInstant()),
                        "\$lt" to Date.from(toDt.toInstant())
                    )
                )),
                D("\$lookup", mapOf(
                    "from" to "event",
                    "let" to mapOf("si" to "\$_id"),
                    "pipeline" to listOf(mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
                        mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
                        mapOf("\$in" to listOf("\$t", listOf(ET.REACT_TAP, ET.REACT_DOUBLE_TAP, ET.REACT_LONG_TAP, ET.REACT_SWIPE, ET.NOACT_TAP, ET.NOACT_DOUBLE_TAP, ET.NOACT_LONG_TAP, ET.NOACT_SWIPE))),
                        mapOf("\$gte" to listOf("\$sgx", 0)),
                        mapOf("\$gte" to listOf("\$sgy", 0)),
                        mapOf("\$ne" to listOf("\$svi", null)),
                        mapOf("\$ne" to listOf("\$sgx", null)),
                        mapOf("\$ne" to listOf("\$sgy", null))
                    ))))),
                    "as" to "event"
                )),
                D("\$unwind", "\$event"),
                D("\$group", mapOf(
                    "_id" to mapOf(
                        "ai" to "\$ai",
                        "av" to "\$av",
                        "t" to "\$event.t",
                        "vhi" to "\$event.ofvhi",
                        "svi" to "\$event.svi",
                        "spx" to mapOf("\$multiply" to listOf(
                            mapOf("\$toInt" to mapOf("\$divide" to listOf(
                                mapOf("\$multiply" to listOf(
                                    "\$event.sgx", mapOf("\$divide" to listOf(720, "\$dw"))
                                )),
                                50
                            ))),
                            50
                        )),
                        "spy" to mapOf("\$multiply" to listOf(
                            mapOf("\$toInt" to mapOf("\$divide" to listOf(
                                mapOf("\$multiply" to listOf(
                                    "\$event.sgy", mapOf("\$divide" to listOf(720, "\$dw"))
                                )),
                                50
                            ))),
                            50
                        )),
                        P.st to mapOf("\$toDate" to mapOf("\$concat" to listOf(
                            mapOf("\$substr" to listOf(
                                mapOf("\$dateToString" to mapOf(
                                    "format" to "%Y-%m-%d %H:%M",
                                    "date" to "\$st"
                                )), 0, 15
                            )),
                            "0:00"
                        )))
                    ),
                    "count" to mapOf("\$sum" to 1)
                )),
                D("\$sort", mapOf("_id.spy" to -1)),
                D("\$addFields", mapOf(
                    "_id.ai" to mapOf(
                        "\$toObjectId" to "\$_id.ai"
                    )
                )),
                D("\$lookup", mapOf(
                    "from" to "app",
                    "localField" to "_id.ai",
                    "foreignField" to "_id",
                    "as" to "app"
                )),
                D("\$replaceWith", mapOf("\$mergeObjects" to listOf(mapOf(
                    "ai" to "\$_id.ai",
                    "av" to "\$_id.av",
                    "st" to "\$_id.st",
                    "stz" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                        "date" to "\$_id.st",
                        "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                    ))),
                    "stzd" to mapOf("\$toDate" to mapOf("\$dateToString" to mapOf(
                        "format" to "%Y-%m-%d 00:00:00",
                        "date" to "\$_id.st",
                        "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
                    ))),
                    "vhi" to "\$_id.vhi",
                    "svi" to "\$_id.svi",
                    "t" to "\$_id.t",
                    "spx" to "\$_id.spx",
                    "spy" to "\$_id.spy",
                    "bft" to mapOf("\$toDate" to Date.from(fromDt.toInstant())),
                    "count" to "\$count"
                )))),
//                D("\$merge", mapOf(
//                    "into" to IndicatorHeatmapByScrollView.COLLECTION_NAME_PT10M,
//                    "on" to listOf("st", "ai", "av", "vhi", "t", "svi", "spx", "spy"),
//                    "whenMatched" to listOf(mapOf("\$addFields" to mapOf(
//                        "st" to "\$st",
//                        "ai" to "\$ai",
//                        "av" to "\$av",
//                        "vhi" to "\$vhi",
//                        "svi" to "\$svi",
//                        "t" to "\$t",
//                        "spx" to "\$spx",
//                        "spy" to "\$spy",
//                        "stz" to "\$\$new.stz",
//                        "stzd" to "\$\$new.stzd",
//                        "bft" to "\$\$new.bft",
//                        "count" to mapOf("\$sum" to listOf("\$count", "\$\$new.count"))
//                    ))),
//                    "whenNotMatched" to "insert"
//                ))
            )
        )).collectList().map { println(">> ${Util.toPretty(it)}"); Status.status200Ok(it) }
    }
    fun sessionRank (req:SafeRequest): Mono<Map<String, Any>> {
        val zt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zt.minusDays(30)
        val toDt = zt.plusDays(1)
        println("Date >> ${Date.from(fromDt.toInstant())}, ${Date.from(toDt.toInstant())}")
        return Flux.from(MongodbUtil.getCollection(IndicatorAllByView.COLLECTION_NAME_PT24H).aggregate(
            listOf(
                D("\$match", mapOf(
                    P.stz to mapOf(
                        "\$gte" to Date.from(fromDt.toInstant()),
                        "\$lt" to Date.from(toDt.toInstant())
                    ),
//                    P.vhi to mapOf("\$ne" to 74180013),
//                    P.vhi to mapOf("\$ne" to -1671032428)
                )),
                D("\$group", mapOf(
                    "_id" to mapOf(
                        "vhi" to "\$vhi",
                    ),
                    P.sco to mapOf("\$sum" to "\$${P.sco}")
                )),
                D("\$group", mapOf(
                    "_id" to mapOf(
                        P.sco to "\$${P.sco}",
                    ),
                    "arr" to mapOf(
                        "\$push" to mapOf(P.vhi to "\$_id.vhi")
                    )
                )),
                D("\$sort", mapOf("_id.${P.sco}" to -1)),
                D("\$group", mapOf(
                    "_id" to mapOf(
                        P.sco to "\$${P.sco}",
                    ),
                    "arr" to mapOf(
                        "\$push" to mapOf(P.sco to "\$_id.${P.sco}")
                    )
                )),

            )
        )).collectList().map { println(">> ${Util.toPretty(it)}"); Status.status200Ok(it) }
    }
    
    fun dwellRefac(req: SafeRequest): Mono<Map<String,Any>>{
        val zt = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val fromDt = zt.minusDays(30)
        val toDt = zt.plusDays(1)
        println("Date >> ${Date.from(fromDt.toInstant())}, ${Date.from(toDt.toInstant())}")

        return Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(listOf(
            D(
                "\$match", mapOf(
                    "\$or" to listOf(
                        mapOf(
                            "i" to mapOf(
                                "\$gte" to Date.from(fromDt.toInstant()),
                                "\$lt" to Date.from(toDt.toInstant())
                            )
                        ),
                        mapOf(
                            "\$and" to listOf(
                                mapOf("i" to mapOf("\$exists" to false)),
                                mapOf(
                                    "st" to mapOf(
                                        "\$gte" to Date.from(fromDt.minusDays(1).toInstant()),
                                        "\$lt" to Date.from(toDt.minusDays(1).toInstant())
                                    )
                                )
                            )
                        )
                    )
                )
            ),
//            D(
//                "\$lookup", mapOf(
//                    "from" to "event",
//                    "let" to mapOf("si" to "\$_id"),
//                    "pipeline" to listOf(
//                        mapOf(
//                            "\$match" to mapOf(
//                                "\$expr" to mapOf(
//                                    "\$and" to listOf(
//                                        mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
//                                        mapOf("\$in" to listOf("\$t", listOf(ET.VIEW_START, ET.VIEW_END)))
//                                    )
//                                )
//                            )
//                        )
//                    ),
//                    "as" to "event"
//                )
//            ),
//            D("\$unwind", "\$event"),
            D("\$lookup", mapOf(
                "from" to "event",
                "localField" to "_id",
                "foreignField" to "_id.${P.si}",
                "as" to "event"
            )),
            D("\$project", mapOf(
                "_id" to 1,
                P.si to 1,
                P.ai to 1,
                P.av to 1,
                P.vhi to 1,
                P.uts to 1,
                "event" to 1,
                "startEndView" to mapOf("\$filter" to mapOf(
                    "input" to "\$event",
                    "as" to "event",
                    "cond" to mapOf("\$in" to listOf("\$\$event.t", listOf(ET.VIEW_START, ET.VIEW_END)))
                )),
                "AppForBack" to mapOf("\$filter" to mapOf(
                    "input" to "\$event",
                    "as" to "event",
                    "cond" to mapOf("\$in" to listOf("\$\$event.t", listOf(ET.APP_FOREGROUND, ET.APP_BACKGROUND)))
                )),
            )),
//            D(
//                "\$group", mapOf(
//                    "_id" to mapOf(
//                        "si" to " \$si",
//                        "ai" to "\$ai",
//                        "av" to "\$av",
//                        "vhi" to "\$event.ofvhi",
//                        "uts" to "\$event.uts",
//                        "st" to mapOf(
//                            "\$toDate" to mapOf(
//                                "\$concat" to listOf(
//                                    mapOf(
//                                        "\$substr" to listOf(
//                                            mapOf(
//                                                "\$dateToString" to mapOf(
//                                                    "format" to "%Y-%m-%d %H:%M",
//                                                    "date" to "\$st"
//                                                )
//                                            ),
//                                            0, 15
//                                        )
//                                    ),
//                                    "0:00"
//                                )
//                            )
//                        )
//                    ),
//                    "min_time" to mapOf("\$min" to "\$event._id.ts"),
//                    "max_time" to mapOf("\$max" to "\$event._id.ts")
//                )
//            ),
//            D(
//              "\$project", D(mapOf(
//                    "dt" to mapOf("\$subtract" to listOf("\$max_time", "\$min_time"))
//                ))
//            ),
//            D("\$group", D(mapOf(
//                "_id" to mapOf(
//                    "st" to "\$_id.st",
//                    "ai" to "\$_id.ai",
//                    "av" to "\$_id.av",
//                    "si" to "\$_id.si",
//                    "vhi" to "\$_id.vhi"
//                ),
//                "dt" to mapOf("\$sum" to "\$dt")
//            ))),
//            D(
//                "\$addFields", mapOf(
//                    "_id.ai" to mapOf("\$toObjectId" to "\$_id.ai")
//                )
//            ),
//            D(
//                "\$lookup", mapOf(
//                    "from" to "app",
//                    "localField" to "_id.ai",
//                    "foreignField" to "_id",
//                    "as" to "app"
//                )
//            ),
//            D(
//                "\$replaceWith", mapOf(
//                    "\$mergeObjects" to listOf(
//                        mapOf(
//                            "ai" to "\$_id.ai",
//                            "av" to "\$_id.av",
//                            "st" to "\$_id.st",
//                            "vhi" to "\$_id.vhi",
//                            "dt" to "\$dt",
//                            "stz" to mapOf(
//                                "\$toDate" to mapOf(
//                                    "\$dateToString" to mapOf(
//                                        "date" to "\$_id.st",
//                                        "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
//                                    )
//                                )
//                            ),
//                            "stzd" to mapOf(
//                                "\$toDate" to mapOf(
//                                    "\$dateToString" to mapOf(
//                                        "format" to "%Y-%m-%d 00:00:00",
//                                        "date" to "\$_id.st",
//                                        "timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
//                                    )
//                                )
//                            ),
//                            "bft" to mapOf("\$toDate" to fromDt.format(BatchUtil.toDateFormater)),
//                        )
//                    )
//                )
//            ),
//          D(
//              "\$merge", mapOf(
//                  "into" to IndicatorAllByView.COLLECTION_NAME_PT10M,
//                  "on" to listOf("st", "ai", "av", "vhi"),
//                  "whenMatched" to "merge",
//                  "whenNotMatched" to "insert"
//              )
//          )
        ))).collectList().map { println(">> ${Util.toPretty(it)}");Status.status200Ok(it) }
    }
}