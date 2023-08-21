package io.userhabit.polaris.service

import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.userhabit.common.*
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.util.*
import org.bson.Document as D

/**
 *
 * @author nsb
 */
object NoticeService {
	private val log = Loggers.getLogger(this.javaClass)
	val COLLECTION_NAME = "notice"
	// @deprecated
	// lateinit var FIELD_LIST: List<Map<String, String>>

	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val ids = req.splitParamOrDefault("ids","")

		val sortField = req.getQueryOrDefault("sort_field", "created_date")
		val sortValue = req.getQueryOrDefault("sort_value", -1)
		val skip = req.getQueryOrDefault("skip",0)
		val limit = req.getQueryOrDefault("limit", 20)
		val fieldList = req.splitQueryOrDefault("field_list", "")
		val searchExpr = req.getQueryOrDefault("search_expr",)

		val vali = Validator()
			.new(limit, "limit").max(100)
		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Flux
			.from(MongodbUtil.getCollection(COLLECTION_NAME)
				.find( D().let{
					if(searchExpr.isNotEmpty()) it.putAll(D.parse(searchExpr))// 이 로직은 반드시 아래 조건 추가 로직보다 우선되어야 함
					if(ids.first() != "*") it.append("_id", D("\$in", ids.map { ObjectId(it) }) )
					it
				} )
				.sort(D(sortField, sortValue))
				.skip(skip)
				.limit(limit)
				.projection( fieldList.fold(D()){ acc, it -> acc.append(it, 1)}
					.let {
						it.append("_id", D("\$toString", "\$_id"))
						if(it.containsKey("created_date")) it.append("created_date", D("\$dateToString", D("date", "\$created_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						if(it.containsKey("updated_date")) it.append("updated_date", D("\$dateToString", D("date", "\$updated_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						it
					})
			)
			.collectList()
			.map {
				Status.status200Ok(it)
			}
	} //  end of get()

	fun postAndPut(req: SafeRequest): Mono<Map<String,Any>> {
		val ids = req.splitParamOrDefault("ids")
		val isPost = req.method() == "POST"
		val bodyList =  try { req.getBodyJsonToList() }catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}
		val vali = Validator()

		bodyList.forEach {
			if(!isPost) vali.new(it, "id").required()
			vali.new(it, "title").required()
			vali.new(it, "content").required()
//			vali.new(it, "is_popup").required(listOf("true", "false"))
		}
		vali.new(bodyList, "body").required()

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		val updateModels = bodyList.map { body ->
			val updateDoc = D()
				.append("title",          body["title"])
				.append("content",        body["content"])
				.append("writer_email",		req.getUserId())
				.append("updated_date",		Date())
				.append("is_popup",		    body["is_popup"] == "true")
			if(isPost){
				updateDoc.append("created_date", Date())
				updateDoc.append("read_count", 0)
			}

			UpdateOneModel<D>(
				D("_id", if(isPost) ObjectId() else ObjectId(body["id"].toString())),
				D("\$set", updateDoc),
				UpdateOptions().upsert(true)
			)
		}

		return Mono
			.from (MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(updateModels))
			.map {
				Status.status200Ok(it)
			}
	} // end of postAndPut()

}
