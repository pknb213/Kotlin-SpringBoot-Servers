package io.userhabit.polaris.service

import io.userhabit.batch.indicators.ObjectList
import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.*
import org.bson.types.ObjectId
import reactor.core.publisher.Mono
import reactor.util.Loggers
import org.bson.Document as D

/**
 * view_list_pt24h에 저장된 Document에 alias 필드를 조회 및 변경합니다.
 */
object AliasService {
    private val log = Loggers.getLogger(this.javaClass)
    // @deprecated
    // lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>
    const val COLLECTION_NAME = "alias"

    /**
     * @author yj
     * @sample
     * @return
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {

        return Mono.just(mapOf("test" to 1))
    }

    /**
     * @author yj
     * @sample POST {{localhost}}/v3/alias
     * @param
     *      app_id_list : App Id List
     *      version_list : Version list ex) 1.0.1, 0.0.1
     *      vhi : View hash id
     *      alias : Alias.
     *      (Option) object_id : If is Ture, aggregatation object_list_pt24h.
     * content-type: text/plain
     * [{
     *    "app_id_list": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
     *    "version_list": "3.6.1",
     *    "vhi": -1876187308,
     *    "alias": "Alias test"
     * }]
     * @return
     * "data": [{
     *   "app_id": "000000000000000000000004",
     *   "app_version": "1.0.1",
     *   "view_hash_id": 1045769909,
     *   "image_path": "s3://service-uh-polaris-stage-2/attach/000000000000000000000004/1.0.1/b5b6a6945c35f47c25dd21cc3ec044d8",
     *   "view_id": "(BrunchDetailActivity)(wv)brunch_detail_webview\"http://userhabit.io/\"",
     *   "alias": "Alias test"
     * }]
     */
    fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
        val bodyList =  try { req.getBodyJsonToList() }
        catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}

        val validator = Validator()
        val email = req.getUserId()
        val isPost = req.method() == "POST"

        validator.new(bodyList, "body").required()
        if (isPost) { // post
            bodyList.forEach {
                validator.new(it, "app_id_list").required()
                validator.new(it, "version_list").required()    // need only 1 version, list is not needed, but to fit the parameter into the format of other apis
                validator.new(it, "vhi").required()
                validator.new(it, "alias").required()
//                validator.new(it, "vi").required()
            }
        } else {
            throw PolarisException.status400BadRequest("Isn't Post !!")
        }
        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())
        val findModel = D()
        var alias = ""
        bodyList.map{
            if (it.containsKey("object_id")){
                findModel.append("oi", it.get("object_id").toString())
            }else if(it.containsKey("oi")) {
                throw PolarisException.status400BadRequest("Please Check, oi is bad keyword. (object_id)")
            }
            val vlist = it.get("version_list").toString()   // for the case such as "2.3.4,2.4.5"

            findModel.append("ai", ObjectId(it.get("app_id_list") as String))
            findModel.append("av", vlist.split(",".toRegex())[0])
            findModel.append("vhi", it.get("vhi"))
            alias = it.get("alias").toString()
        }
        val updateModel = listOf(D(
            "\$addFields", mapOf("alias" to alias)
        ))
        val coll = if (findModel.containsKey("oi")) ObjectList.COLLECTION_NAME_PT24H else ViewList.COLLECTION_NAME_PT24H
        return Mono.from(MongodbUtil.getCollection(coll).findOneAndUpdate(findModel, updateModel))
            .defaultIfEmpty(
                D(mapOf("message" to "Invalid Parameter.'(App ID', 'App Version', 'View Hash ID or Object ID)"))
            )
            .map {
                Status.status200Ok(_keyCasting(it.append("alias", alias)))
            }
    }

    private fun _keyCasting(doc: D): List<D> {
        val map = mapOf(
            "ai" to "app_id",
            "av" to "app_version",
            "vhi" to "view_hash_id",
            "path" to "image_path",
            "vi" to "view_id",
            "alias" to "alias",
            "oi" to "object_id"
        )
        if (doc.containsKey("ai")) doc["ai"] = doc["ai"].toString()
        doc.remove("_id")
        return listOf(D(doc.mapKeys {
            map[it.key]
        }))
    }
    // bookmark와 같이 alias 컬렉션이 필요할 때, 아래 처럼 생성.
    /*fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
        val bodyList =  try { req.getBodyJsonToList() }
        catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}

        val validator = Validator()
        val email = req.getUserId()
        val isPost = req.method() == "POST"

        validator.new(bodyList, "body").required()
        if (isPost) { // post
            bodyList.forEach {
                validator.new(it, "type").required()
                validator.new(it, "uid").required()
            }
        } else {
            throw PolarisException.status400BadRequest("Isn't Post !!")
        }
        if (validator.isNotValid())
            throw PolarisException.status400BadRequest(validator.toExceptionList())

        return Mono.from<Map<String, Any>?>(MongodbUtil.getCollection(COLLECTION_NAME).findOneAndUpdate())
            .defaultIfEmpty(
                D()
            )
            .flatMap {

            }
            .map { Status.status200Ok() }
    }*/
}