package io.userhabit.polaris.service

import com.mongodb.client.model.UpdateOptions
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.service.replay.ReplayServiceReplayList
import io.userhabit.polaris.service.session.SessionServiceSessCount
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*
import org.bson.Document as D
import org.bson.BSONException
import io.userhabit.polaris.Protocol as P
import io.userhabit.polaris.EventType as ET

/**
 * @author nsb
 */
object ReplayService {
    private val log = Loggers.getLogger(this.javaClass)
    const val COLLECTION_NAME = "replay"
    // @deprecated
    // lateinit var FIELD_LIST: List<Map<String, String>>

    /**
     * @comment 22.02.07 yj
     * @sample
     * 	GET {{localhost}}/v3/replay/{ids}?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z&sort_field=session_time&sort_value=-1&search_expr={}
     * @param
     * 	app_id_list: App 컬렉션 _id List
     * 	total_count: true면 전체 Doc 수 출력.
     * @return
     * 	data=[{
    "session_time": "2022-01-14T07:10:18.603Z",
    "session_exp": 114,
    "dwell_time": 2240827.0,
    "view_count": 2,
    "unique_view_count": 2,
    "event_count": 0,
    "device_name": "goldfish_x86 AOSP on IA Emulator",
    "device_os": 101,
    "device_os_ver": "9",
    "app_version": "1.0.1",
    "session_net": 2,
    "session_id": "631fdf93dfd5efba170d871ca9020bc853e63d1b",
    "device_lang": "en_US",
    "crash_id": []
     * }]
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids")
        val appIdList = req.splitQueryOrDefault("app_id_list")
        val fieldList = req.splitQueryOrDefault("field_list")
        val fromDate = req.getQueryOrDefault("from_date")
        val toDate = req.getQueryOrDefault("to_date")
        val sortField = req.getQueryOrDefault("sort_field", "session_time")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 100)
        val searchExpr = req.getQueryOrDefault("search_expr", "{}")
        val deviceId = req.getQueryOrDefault("device_id", "")
        val totalCountFlag = req.getQueryOrDefault("total_count", "false").toBoolean()

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
//			.new(versionList, "version_list").required() // 'version_list='인 경우도 수행하도록 변경
            .new(ids, "ids").required()
            .new(appIdList, "app_id_list").required()
            .new(searchExpr, "search_expr").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        return AppService.getAppIdList(P.uiK, req.getUserId())
            .flatMap { ownedAppIdList ->
                val appIdList = if (appIdList.isNotEmpty() && appIdList.first() != "*") appIdList.filter {
                    ownedAppIdList.contains(it)
                } else ownedAppIdList
                val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
                val searchDoc = try {
                    D.parse(searchExpr)
                } catch (e: BSONException) {
                    throw PolarisException.status400BadRequest(listOf(mapOf("search_expr" to e.message)))
                }

                val pipeline = mutableListOf(
                    D("\$match", D().let { matchDoc ->
                        matchDoc.append(
                            P.st, D()
                                .append("\$gte", Date.from(Instant.parse(fromDate)))
                                .append("\$lt", Date.from(Instant.parse(toDate)))
                        )
                            .append(P.ai, D("\$in", appIdInObjIds))

                        if (!Level.hasPermission(req.getLevel(), Level.SYSTEM)) matchDoc.append(
                            P.ai,
                            D("\$in", QueryPeasant.convertToObjectIdList(appIdList))
                        )
                        if (searchDoc.containsKey(P.aiK)) matchDoc.append(P.ai, searchDoc[P.aiK])
                        if (searchDoc.containsKey(P.avK)) matchDoc.append(P.av, searchDoc[P.avK])
                        if (searchDoc.containsKey(P.seK)) matchDoc.append(
                            P.se,
                            searchDoc[P.seK]
                        ) //session_exp={$gte=1 $lt=2}
                        if (searchDoc.containsKey(P.dnK)) matchDoc.append(P.dn, searchDoc[P.dnK]) //device_name={$eq=a1}
                        if (searchDoc.containsKey(P.snK)) matchDoc.append(P.sn, searchDoc[P.snK]) //session_net={$eq=2}
                        if (searchDoc.containsKey(P.dlK)) matchDoc.append(P.dl, searchDoc[P.dlK]) //device_lang={$eq=c3}
                        if (deviceId.isNotEmpty()) {
                            matchDoc.append(P.di, deviceId)
                        }

                        matchDoc
                    }),
                    D(
                        "\$lookup", D()
                            .append("from", "event")
                            .append("localField", "_id")
                            .append("foreignField", "_id.${P.si}")
                            .append("as", "event")
                    ),
                    D(
                        mapOf(
                            "\$project" to mapOf(
                                "_id" to 1,
                                P.st to 1,
                                P.se to 1,
                                P.di to 1,
                                P.dn to 1,
                                P.`do` to 1,
                                P.dov to 1,
                                P.av to 1,
                                P.sn to 1,
                                "i${P.se}" to 1,
                                P.dl to 1,
                                P.dz to 1,
                                P.dw to 1,
                                P.dh to 1,
                                "startEndView" to mapOf(
                                    "\$filter" to mapOf(
                                        "input" to "\$event",
                                        "as" to "event",
                                        "cond" to mapOf(
                                            "\$in" to listOf(
                                                "\$\$event.t",
                                                listOf(ET.VIEW_START, ET.VIEW_END)
                                            )
                                        )
                                    )
                                ),
                                "startView" to mapOf(
                                    "\$filter" to mapOf(
                                        "input" to "\$event",
                                        "as" to "event",
                                        "cond" to mapOf("\$eq" to listOf("\$\$event.${P.t}", ET.VIEW_START))
                                    )
                                ),
                                "actEvent" to mapOf(
                                    "\$filter" to mapOf(
                                        "input" to "\$event",
                                        "as" to "event",
                                        "cond" to mapOf(
                                            "\$in" to listOf(
                                                "\$\$event.${P.t}",
                                                listOf(
                                                    ET.NOACT_TAP,
                                                    ET.REACT_TAP,
                                                    ET.REACT_LONG_TAP,
                                                    ET.REACT_DOUBLE_TAP,
                                                    ET.REACT_SWIPE,
                                                    ET.SCROLL_CHANGE
                                                )
                                            )
                                        )
                                    )
                                ),
                                "crashEvent" to mapOf(
                                    "\$filter" to mapOf(
                                        "input" to "\$event",
                                        "as" to "event",
                                        "cond" to mapOf("\$eq" to listOf("\$\$event.${P.t}", ET.CRASH))
                                    )
                                ),
                                "backgroundEvent" to mapOf(
                                    "\$filter" to mapOf(
                                        "input" to "\$event",
                                        "as" to "event",
                                        "cond" to mapOf("\$eq" to listOf("\$\$event.${P.t}", ET.APP_BACKGROUND))
                                    )
                                ),
                                // Add odk odv
//						"optionDataEvent" to mapOf("\$filter" to mapOf(
//							"input" to "\$event",
//							"as" to "event",
//							"cond" to mapOf("\$eq" to listOf("\$\$event.${P.t}", ET.USER_CONTENT))
//						))
                            )
                        )
                    ),
                    D(mapOf("\$unwind" to "\$startView")),
                    D(
                        mapOf(
                            "\$unwind" to mapOf(
                                "path" to "\$crashEvent",
                                "preserveNullAndEmptyArrays" to true
                            )
                        )
                    ),
//					D(mapOf("\$unwind" to mapOf(
//						"path" to "\$optionDataEvent",
//						"preserveNullAndEmptyArrays" to true
//					))),
                    D(
                        mapOf(
                            "\$group" to mapOf(
                                "_id" to mapOf(
                                    "_id" to "\$_id",
                                    P.st to "\$st",
                                    P.se to "\$se",
                                    P.di to "\$di",
                                    P.dn to "\$dn",
                                    P.`do` to "\$${P.`do`}",
                                    P.dov to "\$dov",
                                    P.av to "\$av",
                                    P.sn to "\$sn",
                                    "i${P.se}" to "\$i${P.se}",
                                    P.dl to "\$dl",
                                    P.dz to "\$dz",
                                    P.dw to "\$dw",
                                    P.dh to "\$dh",
                                    "maxDt" to mapOf(
                                        "_id" to mapOf("\$first" to "\$startEndView._id.si"),
                                        "dt" to mapOf(
                                            "\$max" to mapOf(
                                                "\$concatArrays" to listOf(
                                                    "\$startEndView._id.ts",
                                                    "\$backgroundEvent._id.ts"
                                                )
                                            )
                                        ),
                                    ),
                                    "ecoList" to mapOf("eco" to mapOf("\$size" to "\$actEvent"))
                                ),
                                P.vco to mapOf("\$push" to "\$startView.vi"),
                                P.uvco to mapOf("\$addToSet" to "\$startView.vi"),
                                P.ci to mapOf("\$addToSet" to "\$crashEvent.ci"),
                                // Todo Add: Not User optionDataEvent !
                            )
                        )
                    ),
                    D(
                        "\$replaceWith", D(
                            D(
                                "\$mergeObjects", listOf(
                                    D()
                                        .append(
                                            P.stK,
                                            D(
                                                "\$dateToString",
                                                D("date", "\$_id.${P.st}").append("format", "%Y-%m-%dT%H:%M:%S.%LZ")
                                            )
                                        )
                                        .append(P.seK, "\$_id.${P.se}")
//								.append(P.dtK, D("\$floor", D("\$divide", listOf("\$_id.maxDt.${P.dt}", 1000))))
                                        .append(P.dtK, "\$_id.maxDt.${P.dt}")
                                        .append(P.vcoK, D("\$size", "\$${P.vco}"))
                                        .append(P.uvcoK, D("\$size", "\$${P.uvco}"))
                                        .append(P.ecoK, D("\$ifNull", listOf("\$_id.ecoList.${P.eco}", 0)))
                                        .append(P.diK, "\$_id.${P.di}")
                                        .append(P.dnK, "\$_id.${P.dn}")
                                        .append(P.doK, "\$_id.${P.`do`}")
                                        .append(P.dovK, "\$_id.${P.dov}")
                                        .append(P.avK, "\$_id.${P.av}")
                                        .append(P.snK, "\$_id.${P.sn}")
                                        .append("i${P.se}", "\$_id.i${P.se}") // TODO isc?
                                        .append(P.siK, "\$_id._id")
                                        .append(P.dlK, "\$_id.${P.dl}")
                                        .append(P.dzK, "\$_id.${P.dz}")
                                        .append(
                                            P.dwhK, D(
                                                "\$concat",
                                                listOf(
                                                    D("\$toString", "\$_id.${P.dw}"),
                                                    "x",
                                                    D("\$toString", "\$_id.${P.dh}")
                                                )
                                            )
                                        )
                                        .append(P.ciK, D("\$ifNull", listOf("\$${P.ci}", emptyList<String>())))
                                )
                            )
                        )
                    ),
                    D().let { matchAfterDoc ->
                        if (searchDoc.containsKey(P.dtK)) matchAfterDoc.append(
                            P.dtK,
                            searchDoc[P.dtK]
                        ) //dwell_time={$gte=3 $lt=4}
                        if (searchDoc.containsKey(P.ecoK)) matchAfterDoc.append(
                            P.ecoK,
                            searchDoc[P.ecoK]
                        ) //event_count={$gte=5 $lt=6}
                        if (searchDoc.containsKey(P.vcoK)) matchAfterDoc.append(
                            P.vcoK,
                            searchDoc[P.vcoK]
                        ) //view_count={$gte=7 $lt=8}
                        if (searchDoc.containsKey(P.uvcoK)) matchAfterDoc.append(
                            P.uvcoK,
                            searchDoc[P.uvcoK]
                        ) //unique_view_count={$gte=9 $lt=10}
                        if (searchDoc.containsKey(P.ciK)) matchAfterDoc.append(
                            P.ciK,
                            searchDoc[P.ciK]
                        ) //crash_id={$eq=g1}

                        D("\$match", matchAfterDoc)
                    },
                    D("\$sort", D(sortField, sortValue)),
                    D("\$skip", skip),
                    D("\$limit", limit),
                )
                if (totalCountFlag) {
                    pipeline.remove(D("\$sort", D(sortField, sortValue)))
                    pipeline.remove(D("\$skip", skip))
                    pipeline.remove(D("\$limit", limit))
                    pipeline.add(D("\$count", "totalDocs"))
                }
                // println(Util.toPretty(pipeline))
                Flux
                    .from(
                        MongodbUtil.getCollection(SessionService.COLLECTION_NAME)
                            .aggregate(pipeline)
                            .allowDiskUse(true)
//							.explain(ExplainVerbosity.EXECUTION_STATS)
                    )
                    .collectList()
                    .map {
//						println(Util.toPretty(it))
                        Status.status200Ok(it)
                    }
            }
    } // end of get()

    /**
     * @author yj
     * @sample [POST {{localhost}}/v3/replay]
     * @body [key=value&key=value]
     * @param [user & token, session id]
     */
    fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
        """
			사용 안함
		""".trimIndent()
        val user = req.getBodyOrDefault("user", "")
        val si = req.getBodyOrDefault(P.si, "")
//		println("-> Body $user, $si")

        val vali = Validator()
            .new(user, "user").required()
            .new(si, P.siK).required()

        if (vali.isNotValid()) return Mono.just(
            Status.status400BadRequest(
                Message.get(req.lang, Message.badRequest),
                vali.toExceptionList()
            )
        )

        """
		즐겨찾기 버튼을 눌렀을때, SI 값과 User 값을 받아서
		해당 값을 DB에 저장하는 식으로 
			
		""".trimIndent()
        return Mono.from(
            MongodbUtil.getCollection(BookmarkService.COLLECTION_NAME)
                .updateOne(
                    D(mapOf("si" to si, "view_type" to COLLECTION_NAME)),
                    D(mapOf("\$mul" to mapOf("show" to -1))),
                    UpdateOptions().upsert(true)
                )
        )
            .doOnError {
                log.info(Util.toJsonString(listOf(COLLECTION_NAME, user, si)))
            }
            .map {
                Status.status200Ok(it, listOf(mapOf("User" to user, P.siK to si)), "Done")
            }
    }


    fun getReplayList(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list")
        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date")
        val toDate = req.getQueryOrDefault("to_date")
        val searchExpr = req.getQueryOrDefault("search_expr", "{}")
        val getTotalCount = req.getQueryOrDefault("total_count", "false").toBoolean()

        val sortField = req.getQueryOrDefault("sort_field", "session_time")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 100)

        val deviceId = req.getQueryOrDefault("device_id", "")

        val vali = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(appIdList, "app_id_list").required()
            .new(searchExpr, "search_expr").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = AppService.getAppIdList(P.uiK, req.getUserId())

        val replayListServ = ReplayServiceReplayList()

        val mongoResStream = if (getTotalCount) {
            replayListServ.getMongoResStreamReplayListTotalCount(
                appIdStream, appIdList, versionList, fromDate, toDate, deviceId, searchExpr
            )
        } else {
            replayListServ.getMongoResStreamReplayList(
                appIdStream, appIdList, versionList,
                fromDate, toDate, deviceId, searchExpr,
                sortField, sortValue, skip, limit
            )
        }
        val respStream = mongoResStream.map { result ->
            log.debug(QueryPeasant.listDocToStr(result))
            Status.status200Ok(result)
        }
        return respStream
    }

}
