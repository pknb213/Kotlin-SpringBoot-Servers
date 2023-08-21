package io.userhabit.polaris.service

import com.mongodb.ExplainVerbosity
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.internal.operation.FindOperation
import io.userhabit.batch.BatchUtil
import io.userhabit.batch.indicators.IndicatorByFlow
import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import io.userhabit.polaris.Protocol as P
import org.bson.Document as D

object BookmarkService {
    private val log = Loggers.getLogger(this.javaClass)
    const val COLLECTION_NAME = "bookmark"
    // @deprecated
    // lateinit var FIELD_LIST: List<Map<String, String>>

    // Todo : User 별로 접근하여 즐겨찾기 리스트를 보여 줘야 한다. AUth는 다음 문제.
    /**
    현재 즐겨 찾기 기능이 필요한 항목은 세 가지이다.
    View, Replay, Target flow 3가지 이다.

    - View의 경우 기존에는 View_list 컬렉션의 Favorite 필드로 즐겨 찾기 기능을 동작 했지만
    다른 Favorite과 API 기능적으로 연동이 되지 않으며 User 별로 할 방법이 없다.
    고유 뷰를 얻기 위해서 si, vhi, ts가 필요한 것으로 보인다.
     * API: /v3/view
     * 참조하는 컬렉션: view_list_pt24h
     * 필드 리스트: _id, ai, av, favorite, vhi, vi
     * 유니크 키: ai, av, vhi
     * 결과 값: 저장된 뷰 즐겨찾기 목록
     * 즐겨 찾기 추가할 때: view list 페이지에서 추가를 누르면 => 유니크 키만 등록?

    - Replay의 경우 Aggregate 함으로서 얻는 값으로 고유 리플레이 값을 저장 하기 위해서는 si 값이 필요한 것으로 보인다.
     * API: /v3/replay
     * 참조하는 컬렉션: bookmark, session, event(lookup 3번)
     * 유니크 키: _id
     * 참조 필드: ai , av, se (SessionExperience), dn(device name), dl(device language) ,sn(session network)
     * 결과 값: 저장된 리플레이 즐겨 찾기 목록
     * 즐겨 찾기 추가 할 때: replay view 페이지에서 추가를 누르면 => 유니크 키만 등록한다?

    - Target Flow의 경우 indicator_by_flow 컬렉션의 flow 필드의 맞게 시작과 끝 vhi가 일치하는 경우 데이타를
    가져오는 것으로 확인된다.
     * API: /v3/flow & type=target
     * 참조하는 컬렉션: indicator_by_flow_pt24h
     * 유니크 키: flow 내부 값
     * 참조 필드: stz, ai, vhi (regex: 시작~끝), av
     * 결과 값: 뷰 2개가 저장된 즐겨 찾기 목록
     * 즐겨 찾기 추가할 때: 시작 뷰, 끝 뷰
    -> 시작, 끝 view hash id로 해보려고 했는데, 동일 뷰를 했을 경우 생성되는 정규 표현식은 아래와 같다.
    Document{{$regex=^.*,-1413544682,.*,-1413544682}}
    이렇게 생성되면 indicator_by_flow_pt24h에서 여러 값을 가져온다.
    웹페이지에서는 동일 뷰이기 때문에 1 depth만 출력해야하기 때문에 view_list의 _id를 유니크하게 해야할 지도 모르겠다.

    새로운 북마크 컬렉션
    위의 세 가지 항목에 대한 정보를 다 같이 저장하고 리딩했으면 좋겠다.
    예상 필드와 역할
    - _id: 유니크 키 (CRUD 용)
    - target: 어떤 용도의 즐겨 찾기 인지
    - 유니크 필드: (1개 또는 그 이상)
     */
    /**
     * @author yj
     * @sample GET {{localhost}}/v3/bookmark?email=test01@userhabit.io&type=view
     * @return data=[
        "4444442133222",
        "1,3",
        "4214897,324928"
     * ]
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val ids = req.splitParamOrDefault("ids", "")

        val viewType = req.getQueryOrDefault("type", "")
        val email = req.getQueryOrDefault("email", "")

        val sort_field = req.getQueryOrDefault("sort_field", "created_date")
        val sort_value = req.getQueryOrDefault("sort_value", -1)
        val skip = req.getQueryOrDefault("skip", 0)
        val limit = req.getQueryOrDefault("limit", 20)
        val fieldList = req.splitQueryOrDefault("field_list", "")
        val appIdList = req.splitQueryOrDefault("app_id_list", "")

        val validator = Validator()
            .new(viewType, "type").required(listOf("view","replay","target"))
            .new(appIdList, "app_id_list").required()
            .new(email, "email").required()
            .new(limit, "limit").max(100)

        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)
        val pipeline = listOf(
            D(mapOf("\$match" to mapOf(
                "type" to viewType,
                "email" to email,
                "ai" to D("\$in", appIdInObjIds),
                "show" to true
            ))),
            D(mapOf("\$project" to mapOf(
                "_id" to 0,
                "show" to 0,
                "email" to 0,
            )))
        )

        return Flux
            .from(MongodbUtil.getCollection(COLLECTION_NAME)
                .aggregate(pipeline).allowDiskUse(true)
            )
            .defaultIfEmpty(D())
            .collectList()
            .map{ result ->
                Status.status200Ok(result)
            }
    }

    /**
     * @author yj
     * @sample POST {{localhost}}/v3/bookmark
     * content-type: text/plain
     * [{
     *    "type": "target",
     *     "uid": "2134,109283902"
     * }]
     * @return [{
            "insert_count": 0,
            "match_count": 1,
            "modified_count": 1
        }]
     */
    fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
        val bodyList =  try { req.getBodyJsonToList() }
        catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}

        val validator = Validator()
        val isPost = req.method() == "POST"
        val email = req.getUserId()

        if (isPost) { // post
            bodyList.forEach {
                validator.new(it, "type").required()
                validator.new(it, "uid").required()
                validator.new(it, "app_id_list").required()
                validator.new(it, "show").required()
            }
        } else {
            throw PolarisException.status400BadRequest("Isn't Post !!")
        }

        validator.new(bodyList, "body").required()

        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        val postData = bodyList.get(0)
        val appId = ObjectId(postData["app_id_list"] as String)
        val findModel = D("email", email)
            .append("type", postData["type"])
            .append("uid", postData["uid"])
            .append("ai", appId)

        return Mono
            .from(MongodbUtil.getCollection(COLLECTION_NAME).find(findModel))
            .defaultIfEmpty(
                D().append("email", email)
                    .append("type", postData["type"])
                    .append("uid", postData["uid"])
                    .append("show", postData["show"])
                    .append("ai", appId)
            )
            .flatMap {
                // update the show field
                it.set("show", postData["show"])

                val updateModel = UpdateOneModel<D>(
                    D("_id", if (it.get("_id") == null) ObjectId() else ObjectId(it.get("_id").toString())),
                    D("\$set", D(it)), // update field
                    UpdateOptions().upsert(true)
                )
                Mono.from(MongodbUtil.getCollection(COLLECTION_NAME)
                    .bulkWrite(listOf(updateModel)))
            }
//            .map { Status.status200Ok(it.toString()) }
            .map { Status.status200Ok(listOf(mapOf("insert_count" to if(it.upserts.isNotEmpty()) 1 else 0, "match_count" to it.matchedCount, "modified_count" to it.modifiedCount))) }
    }
}