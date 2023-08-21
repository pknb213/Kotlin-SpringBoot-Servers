package io.userhabit.polaris.service

import io.userhabit.common.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.util.*
import org.bson.Document as D

/**
 * @author nsb
 * @comment 21.11.19 yj
 * @sample [GET {{localhost}}/v3/login_history/{ids}?field_list=data]
 * @return data=[...]
 * {
	"_id": {
		"timestamp": 1636619235,
		"date": 1636619235000
	},
	"member_id": "",
	"ip": "127.0.0.1",
	"created_date": 1636619235409
 * },
 */
object LoginHistoryService {
	private val log = Loggers.getLogger(this.javaClass)
	val COLLECTION_NAME = "login_history"
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
				.find( D().let {
					if(Level.hasPermission(req.getLevel(), Level.SYSTEM)) {
						if(ids.first() != "*") it.append("_id", D("\$in", ids))
					}else if(ids.first() == "*"){
						it.append("_id", D("\$in", listOf(req.getUserId())))
					}else{
						val id = req.getUserId()
						val notIdList = ids.filter { it != id }
						if(notIdList.isNotEmpty())
							throw PolarisException.status403Forbidden(notIdList.map { mapOf("id[$it]" to "") })

						it.append("_id", D("\$in", ids))
					}
					if(searchExpr.isNotEmpty()) it.putAll(D.parse(searchExpr))

					it
				})
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

	fun post(req: SafeRequest): Mono<Map<String,Any>> {
		val id = req.getBodyOrDefault("email", "")
		val vali = Validator()
			.new(id, "id").required().email()

		return Mono
			.from(
				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).insertOne(
					D()
						.append("member_id", id)
//						.append("login_count", (doc["login_count"] as String).toInt().plus(1)) // TODO 보람님 확인 후
						.append("ip", req.getIP())
						.append("created_date", Date())
				))
			)
			.map {
				Status.status200Ok(it)
			}
	} // end of postAndPut()

}
