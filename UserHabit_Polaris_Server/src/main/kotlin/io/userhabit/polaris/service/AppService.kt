package io.userhabit.polaris.service

import com.mongodb.client.model.DeleteOneModel
import com.mongodb.client.model.UpdateManyModel
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.service.app.AppServiceAppStats
import io.userhabit.polaris.Protocol as P
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import org.bson.Document as D

/**
 *  App 정보
 */
object AppService {
    private val log = Loggers.getLogger(this.javaClass)
    const val COLLECTION_NAME = "app"
    // @deprecated
    // lateinit var FIELD_LIST: List<Map<String, String>>

    /**
     * @author sbnoh
     * @comment 21.11.18 yj
     * @sample [GET {{localhost}}/v3/app/{ids}]
     * @return data=[
     * {
    "name": "네이버",
    "created_date": "2020-01-01T12:00:00Z",
    "_id": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc"
     * }, ...
     * ]
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids")
        val fieldList = req.splitQueryOrDefault("field_list", "name,created_date")
        val sortField = req.getQueryOrDefault("sort_field", "created_date")
        val sortValue = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 20)
        val searchExpr = req.getQueryOrDefault("search_expr")

        val validator = Validator()
            .new(limit, "limit").max(100)
        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        return Flux
            .from(MongodbUtil.getCollection(COLLECTION_NAME)
                .find(D().let {
                    if (searchExpr.isNotEmpty()) it.putAll(D.parse(searchExpr))// 이 로직은 반드시 아래 조건 추가 로직보다 우선되어야 함

                    val level = req.getLevel()
                    if (Level.hasPermission(level, Level.SYSTEM)) {
                        if (ids.first() != "*") it.append("_id", D("\$in", QueryPeasant.convertToObjectIdList(ids)))
//						if(ids.first() != "*") it.append("member_id", D("\$in", ids))
                    } else if (ids.first() == "*") {
                        if (Level.hasPermission(level, Level.ADMINISTRATOR) and Level.hasPermission(
                                level,
                                Level.MANAGER
                            )
                        ) {
                            it.append("_id", D("\$in", QueryPeasant.convertToObjectIdList(req.getAppIdList())))
                        } else { // Member 이하 Level
                            it.append("member_id", D("\$in", listOf(req.getUserId())))
                        }
                    } else {
                        it.append("_id", D("\$in", req.getAppIdList().let { ownedList ->
                            ids.filter {
                                ownedList.contains(it)
                            }.map {
                                ObjectId(it)
                            }
                        }
                        ))
                    }
                    it
                })
                .sort(D(sortField, sortValue))
                .skip(skip)
                .limit(limit)
                .projection(fieldList
                    .fold(D()) { doc, key -> doc.append(key, 1) }
                    .let {
                        it.append("_id", D("\$toString", "\$_id"))
                        if (it.containsKey("created_date")) it.append(
                            "created_date",
                            D(
                                "\$dateToString",
                                D("date", "\$created_date").append("format", "%Y-%m-%dT%H:%M:%SZ")
                            )
                        )
                        if (it.containsKey("expiry_date")) it.append(
                            "expiry_date",
                            D(
                                "\$dateToString",
                                D("date", "\$expiry_date").append("format", "%Y-%m-%dT%H:%M:%SZ")
                            )
                        )
//						it.remove(P.akK) // app_key는 절대 노출되면 안됨 // TODO 일단 출력되게 해놨는데 정책이 필요함
                        it
                    }
                )
            )
            .collectList()
            .map {
                Status.status200Ok(it)
            }

    }

    /**
     *  앱 목록
     *  @author LeeJaeun
     *  @comment 21.11.18 yj
     *  @sample [GET {{localhost}}/v3/app/{ids}/count?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z&by=all]
     *  @see [https://github.com/userhabit/uh-issues/issues/386]
     */
    fun count(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitParamOrDefault("ids", "")

        val versionList = req.splitQueryOrDefault("version_list", "")
        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")
        val sortField = req.getQueryOrDefault("sort_field", "_id")
        val sortValue = req.getQueryOrDefault("sort_value", 1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 40)
        val by = req.getQueryOrDefault("by", "")
        val duration = req.getQueryOrDefault("duration", "day")
//		val by = req.getQueryOrDefault("by", "session")
        val type = req.getQueryOrDefault("type", "base")
//		val isUnique = req.getQueryOrDefault("is_unique", "false").toBoolean()
        val coll: String
        val query: List<D>

        val vali = Validator()
//			.new(appIdList, "app_id_list").required()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()
            .new(duration, "duration").required(listOf("all", "day", "week", "month", "year"))
            .new(by, "by").required(listOf("all", "view", "new", "app", "plan", "test", "production", "accumulated"))
            .new(limit, "limit").max(100)

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val createdDateDoc = if (duration == "month") {
            D("\$dateToString", D("format", "%Y-%m-01T00:00:00.000Z").append("date", "\$${P.cdK}"))
        } else if (duration == "week") {
            D(
                "\$toString",
                D(
                    "\$dateFromString",
                    D("format", "%G-%V").append(
                        "dateString",
                        D("\$dateToString", D("format", "%G-%V").append("date", "\$${P.cdK}"))
                    )
                )
            )
        } else if (duration == "day") {
            D("\$dateToString", D("format", "%Y-%m-%dT00:00:00.000Z").append("date", "\$${P.cdK}"))
        } else if (duration == "hour") {
            D("\$dateToString", D("format", "%Y-%m-%dT%H:00:00.000Z").append("date", "\$${P.cdK}"))
        } else if (duration == "year") {
            D("\$dateToString", D("format", "%Y-01-01T00:00:00.000Z").append("date", "\$${P.cdK}"))
        } else { // all
            D()
        }

        // TODO 콘솔에서 각각 호출 후 처리하는 걸로 합의 2020-11-18
        if (type == "base") {
            val reqToDevice = req.getNew(mapOf("by" to listOf("device")), mapOf())
            return Mono
                .zip(
                    DeviceService.count(reqToDevice),
                    SessionService.count(req.getNew(mapOf("by" to listOf("session")), mapOf())),
                    get(req),
                )
                .map { tuple ->
                    val errorList = tuple.filter { (it as Map<String, Any>)["status"] != 200 }
                    if (errorList.isNotEmpty()) {
                        val m = errorList[0] as Map<String, Any>
                        Status.custom(m["status"] as Int, m["message"] as String, m["data"] ?: "")
                    } else {
                        val dauData = tuple.t1["data"] as List<Map<String, Any>>
                        val sessionData = tuple.t2["data"] as List<Map<String, Any>>
////					val crashData = tuple.t3["data"] as List<Map<String, Any>>
                        val appData = tuple.t3["data"] as List<Map<String, Any>>

                        val appMap = appData.associateBy { it[P.aiK] as String }.toMutableMap()

                        dauData.forEach {
                            val ai = it[P.aiK] as String
                            appMap[ai] = appMap[ai]!!.plus(it)
                            // ai값으로 꺼내오면 null 일 수 없다. appIdList를 각 서비스에 전달하기 때문.
                        }

                        sessionData.forEach {
                            val ai = it[P.aiK] as String
                            appMap[ai] = appMap[ai]!!.plus(it)
                        }

//					crashData.forEach {
//						val ai = it[P.aiK] as String
//						appMap[ai] = appMap[aiK]!!.plus(it)
//					}

                        Status.status200Ok(appMap.map { it.value })
                    }
                }
            // end of base
        } else if (by == "new" || by == "plan" || by == "accumulated") {

            val planTypeDoc = when (type) {
                "free" -> D("\$eq", "free")
                "startup" -> D("\$eq", "startup")
                "business" -> D("\$eq", "business")
                "enterprise" -> D("\$eq", "enterprise")
                "paid" -> D("\$in", listOf("enterprise", "free", "startup", "buisness"))
                else -> D()
            }

            val projectDoc = D().append("_id", 0)
                .append(P.cdK, "\$_id.${P.cdK}")
                .append(P.acoK, "\$${P.aco}")

            val matchQuery = if (by == "new") {
                D().append(
                    P.cdK,
                    D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
                )
            } else if (by == "plan") {
                D().append(
                    P.cdK,
                    D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
                ).append(P.ptK, planTypeDoc)
            } else {
                D().append(P.cdK, D("\$lte", Date.from(Instant.parse(toDate))))
            }
            query = listOf(
                D("\$match", matchQuery),
                D(
                    "\$group", D()
                        .append(
                            "_id", D()
                                .append(P.cdK, createdDateDoc)
                        )
                        .append(P.aco, D("\$sum", 1))
                ),
                D("\$project", projectDoc),
                D("\$sort", D(sortField, sortValue)),
                D("\$skip", skip),
                D("\$limit", limit)
            )
//			println(" ${Util.toPretty(query)}")
            return Flux
                .from(
                    MongodbUtil.getCollection(COLLECTION_NAME)
                        .aggregate(query)
                )
                .collectList()
                .map { Status.status200Ok(it) }
        } else if (type == "build_type") {

            val groupQuery = if (by == "test") {
                D(
                    "\$group", D()
                        .append("_id", D().append(P.cdK, createdDateDoc))
                        .append(
                            P.aco,
                            D().append(
                                "\$sum",
                                D().append(
                                    "\$cond",
                                    listOf(
                                        D().append(
                                            "\$eq",
                                            listOf(D().append("\$substr", listOf("\$app_key", 0, 4)), "dev_")
                                        ), 1, 0
                                    )
                                )
                            )
                        )
                )
            } else {
                D(
                    "\$group", D()
                        .append("_id", D().append(P.cdK, createdDateDoc))
                        .append(
                            P.aco,
                            D().append(
                                "\$sum",
                                D().append(
                                    "\$cond",
                                    listOf(
                                        D().append(
                                            "\$eq",
                                            listOf(D().append("\$substr", listOf("\$app_key", 0, 4)), "dev_")
                                        ), 0, 1
                                    )
                                )
                            )
                        )
                )
            }
            val projectDoc = D().append("_id", 0)
                .append(P.cdK, "\$_id.${P.cdK}")
                .append(P.acoK, "\$${P.aco}")

            val matchQuery = D().append(
                P.cdK,
                D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate)))
            )

            query = listOf(
                D("\$match", matchQuery),
                groupQuery,
                D("\$project", projectDoc),
                D("\$skip", skip),
                D("\$limit", limit)
            )
//			println("query : ${Util.toPretty(query)}")
            return Flux
                .from(
                    MongodbUtil.getCollection(COLLECTION_NAME)
                        .aggregate(query)
                )
                .collectList()
                .map {
                    Status.status200Ok(it)
                }
        } else {
            return Mono.just(mapOf("" to ""))
        }
    }

    /**
     * App Collection에 저장된 configuration 필드를 조회하는 함수이다.
     * @author cyj
     * @sample GET {{localhost}}/v3/app/{app key}/config
     * @return
     * 	"data": [{
     * 		"config": {
     * 			"enable": true,
     * 			"image_capture": true
     * 		}
     * 	},]
     */
    fun getConfig(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids", "")

        val validator = Validator()
        validator.new(ids, "ids").required()
        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        val filter = D("app_key", D("\$in", ids))
        return _getAppConfigStream(filter)
    }

    /**
     * App Collection에 저장된 configuration 필드를 업데이트하는 함수이다.
     * @author cyj
     * @sample
     * 		POST {{localhost}}/v3/app/{app key}/config
     * 		Authorization: {{jwt}}
     *		content-type: text/plain
     *
     *		[{"ph": "010-1234-1234", "a": 2}]
     *
     * @return
     * 	"status": 200,
     * 	"message": "OK",
     * 	"data": {
     * 		"Matched Count": 1,
     * 		"Inserted Count": 0,
     * 		"App Key": [
     * 			"userhabit_app_key_04"
     * 		],
     * 		"Configuration": [{
     * 			"ph": "010-1234-1234",
     * 			"a": 2
     * 		}]
     * },
     */
    fun postConfig(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids", "")

        val validator = Validator()
        validator.new(ids, "ids").required()
        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        val bodyList = try {
            req.getBodyJsonToList()
        } catch (e: Exception) {
            throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))
        }
        if (bodyList.isEmpty() || bodyList.size > 1) PolarisException.status400BadRequest(bodyList)
//		bodyList.forEach{
//			// Todo : Validate to field
//		}
        return _updateAppConfigStream(bodyList, D("app_key", mapOf("\$in" to ids)))
    }

    /**
     *  앱 정보 생성, 수정
     *  @author lje
     *  TODO : 수정할 수 있는 항목 확인
     *  @comment 21.11.18 yj
     *  @sample POST {{localhost}}/v3/app
     *  @param "id": "6194bbf1c7b44b662a748e8e",
    "time_zone": "Asia/Seoul",
    "name": "영조쓰",
    "platform": "ios",
    "admin_id": "test01@userhabit.io"
     * @return
     * @see [POST로 보내든 PUT으로 보내든 업데이트하지 않고 새로 추가 함]
     */
    fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
        val bodyList = try {
            req.getBodyJsonToList()
        } catch (e: Exception) {
            throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))
        }
        val vali = Validator()
        val isPost = req.method() == "POST"
        val level = req.getLevel()

        if (!Level.hasPermission(level, Level.ADMINISTRATOR))
            throw PolarisException.status403Forbidden()

        bodyList.forEach {
            if (isPost) {
                vali.new(it, "time_zone").required()
                vali.new(it, "name").required()
                vali.new(it, "platform").required()
            } else {
                vali.new(it, "app_key").containsPermission(Level.ADMINISTRATOR) // TODO 어드민도?? > 기획
                vali.new(it, "id").required()
            }
        }
        vali.new(bodyList, "body").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appKeyList = mutableListOf<String>()
        return Flux
            .fromIterable(
                bodyList
            )
            .map { body ->
                val instant = Instant.now()
                val updateDoc = D()
                body["time_zone"]?.let { updateDoc.append("time_zone", it) }
                body["name"]?.let { updateDoc.append("name", it) }
                body["platform"]?.let { updateDoc.append("platform", it) }
                req.getCompanyID()?.let { updateDoc.append("company_id", ObjectId(it)) }
                updateDoc.append("updated_date", instant)
                if (isPost) {
                    val appKey = UUID.randomUUID().toString()
                    appKeyList.add(appKey)
                    updateDoc.append("admin_id", req.getUserId())
                        .append("expiry_date", instant.plus(14, ChronoUnit.DAYS)) // 무료체험 기간 14일
                        .append("app_key", appKey)
                        .append("session_count", 0L)
                        .append("created_date", instant)
                } else {
                    appKeyList.add(body["app_key"].toString())
                    body["app_key"]?.let { updateDoc.append("app_key", it) }
                }

                UpdateOneModel<D>(
                    D("_id", if (isPost) ObjectId() else ObjectId(body["id"].toString())),
                    D("\$set", updateDoc),
                    UpdateOptions().upsert(true)
                )
            }
            .collectList()
            .flatMap {
                Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(it))
            }
            .map {
                Status.status200Ok(appKeyList)
            }
    }

    fun delete(req: SafeRequest): Mono<Map<String, Any>> {
        val docList = req.getBodyJsonToList()
        val vali = Validator()
        docList.forEach {
            vali.new(it.getOrDefault("_id", "") as String, "_id / ${it.toString()}").required()
        }

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        return Flux
            .fromIterable(
                docList
            )
            .map { doc ->
                doc as MutableMap
                DeleteOneModel<D>(D("_id", ObjectId(doc.remove("_id") as String)))
            }
            .collectList()
            .flatMap {
                Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(it))
            }
            .map {
                Status.status200Ok(it)
            }
    }

    fun getStatsSessionViewDwell(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list", "")
        val versionList = req.splitQueryOrDefault("version_list", "")

        val vali = Validator()
            .new(appIdList, "app_id_list").required()

        if (vali.isNotValid())
            throw PolarisException.status400BadRequest(vali.toExceptionList())

        val appIdStream = this.getAppIdList(P.uiK, req.getUserId())

        val appInfo = AppServiceAppStats()
        val mongoResStream = appInfo.getMongoResStreamAppInfo(
            appIdStream, appIdList, versionList,
        )
        val respStream = mongoResStream.map { result ->
            log.debug(QueryPeasant.listDocToStr(result))
            Status.status200Ok(result)
        }
        return respStream
    }

    private val lruListCache = object : java.util.LinkedHashMap<String, List<String>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>): Boolean {
            return if (super.size > 500) true else false
        }
    }

    fun getAppIdList(
        key: String,
        value: String,
        useCache: Boolean = true,
        searchKey: String = "",
        searchValue: String = ""
    ): Mono<List<String>> {

        return if (useCache) {
            lruListCache[value]
        } else {
            null
        }
            ?.let {
                Mono.just(it)
            }
            ?: Flux
                .from(
                    MongodbUtil
                        .getCollection(COLLECTION_NAME)
                        .find(D(key, value).let {
                            if (searchKey.isNotEmpty() && searchValue.isNotEmpty())
                                it.append(searchKey, D("\$regex", ".*${searchValue}.*"))
                            it
                        })
                        .projection(D("_id", 1))
                )
//			.defaultIfEmpty(D())
                .map {
                    it["_id"].toString() // as String
                }
                .collectList()
                .map {
                    lruListCache[value] = it
                    it
                }
    }

    private val lruCache = object : java.util.LinkedHashMap<String, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean {
            return if (super.size > 500) true else false
        }
    }

    fun getAppIdMap(appKeyList: List<String>): Mono<Map<String, String>> {
        if (appKeyList.isEmpty()) return Mono.just(mapOf())
        val keyPair = appKeyList.partition { lruCache.containsKey(it) }
        val cashedMap = keyPair.first.associateBy({ it }, { lruCache[it]!! })

        return if (keyPair.second.isEmpty()) {
            Mono.just(cashedMap)
        } else Flux
            /** 동일한 app_key가 app에 존재하면 로직이 꼬임. 사실상 Mono ! */
            .from(
                MongodbUtil
                    .getCollection(COLLECTION_NAME)
                    .find(D(P.akK, D("\$in", appKeyList)))
            )
            .collectMap(
                {
                    it[P.akK] as String
                },
                {
                    val id = it["_id"].toString()
                    lruCache[it[P.akK] as String] = id
                    id
                }
            )
            .defaultIfEmpty(mapOf())
            .map {
                cashedMap.plus(it)
            }
    }


    /**
     * @sample GET {{localhost}}/v3/app/config?app_id_list=0000003
     * @return
     * 	"data": [{
     * 		"config": {
     * 			"enable": true,
     * 			"image_capture": true
     * 		}
     * 	},]
     */
    fun getAppConfig(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitQueryOrDefault("app_id_list")

        val validator = Validator()
        validator.new(appIdList, "app_id_list").required()
        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        val filter = D("_id", mapOf("\$in" to QueryPeasant.convertToObjectIdList(appIdList)))
        return _getAppConfigStream(filter)

    }

    /**
     * @sample
     * 		POST {{localhost}}/v3/app/{app key}/config
     * 		Authorization: {{jwt}}
     *		content-type: text/plain
     *
     *		[{"ph": "010-1234-1234", "a": 2}]
     *
     * @return
     * 	"status": 200,
     * 	"message": "OK",
     * 	"data": {
     * 		"Matched Count": 1,
     * 		"Inserted Count": 0,
     * 		"Configuration": [{
     * 			"ph": "010-1234-1234",
     * 			"a": 2
     * 		}]
     * },
     */
    fun postAppConfig(req: SafeRequest): Mono<Map<String, Any>> {
        val appIdList = req.splitParamOrDefault("appIdList", "")

        val validator = Validator()
        validator.new(appIdList, "appIdList").required()
        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        val bodyList = try {
            req.getBodyJsonToList()
        } catch (e: Exception) {
            throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))
        }
        if (bodyList.isEmpty() || bodyList.size > 1) PolarisException.status400BadRequest(bodyList)

        val filter = D("_id", mapOf("\$in" to QueryPeasant.convertToObjectIdList(appIdList)))
        return _updateAppConfigStream(bodyList, filter)

    }


    fun _getAppConfigStream(filter: D): Mono<Map<String, Any>> {

        val match = D("\$match", filter)
        val project = D("\$project", D("_id", 0).append("config", 1))

        return Flux
            .from(
                MongodbUtil.getCollection(COLLECTION_NAME)
                    .aggregate(listOf(match, project))
            )
            .collectList()
            .map {
                Status.status200Ok(it)
            }
            .doOnError {
                Status.status400BadRequest()
            }
    }

    fun _updateAppConfigStream(bodyList: List<Map<String, Any>>, updateFilter: D): Mono<Map<String, Any>> {
        return Flux
            .fromIterable(bodyList)
            .map {
                UpdateManyModel<D>(
                    updateFilter,
                    D("\$set", mapOf("config" to it))
                )
            }
            .collectList()
            .flatMap {
                Mono.from(
                    MongodbUtil.getCollection(COLLECTION_NAME)
                        .bulkWrite(it)
                )
            }
            .map {
                val res = mapOf(
                    "Matched Count" to it.matchedCount,
                    "Inserted Count" to it.insertedCount,
                    "Configuration" to bodyList
                )
                Status.status200Ok(res)
            }
            .doOnError {
                Status.status400BadRequest()
            }
    }

}

