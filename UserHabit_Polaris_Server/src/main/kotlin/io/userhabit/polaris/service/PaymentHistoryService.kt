package io.userhabit.polaris.service

import io.userhabit.common.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import org.bson.Document as D

/**
 *
 * @author nsb
 */
object PaymentHistoryService {
	private val log = Loggers.getLogger(this.javaClass)
	val COLLECTION_NAME = "payment_history"
	// @deprecated
	// lateinit var FIELD_LIST: List<Map<String, String>>

	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val ids = req.splitParamOrDefault("ids",)

		val sortField = req.getQueryOrDefault("sort_field", "created_date")
		val sortValue = req.getQueryOrDefault("sort_value", -1)
		val skip = req.getQueryOrDefault("skip",0)
		val limit = req.getQueryOrDefault("limit", 20)
		val fieldList = req.splitQueryOrDefault("field_list", )
		val searchExpr = req.getQueryOrDefault("search_expr",)

		val vali = Validator()
			.new(limit, "limit").max(100)
			.new(req.getLevel(), "level").containsPermission(Level.SYSTEM, Level.ADMINISTRATOR)
		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Flux
			.from(MongodbUtil.getCollection(COLLECTION_NAME)
				.find( D().let{
					if(searchExpr.isNotEmpty()) it.putAll(D.parse(searchExpr)) // 이 로직은 반드시 아래 조건 추가 로직보다 우선되어야 함

					val level = req.getLevel()
					if(Level.hasPermission(level, Level.SYSTEM)) {
						if(ids.first() != "*") it.append("_id", D("\$in", ids))
					}else{
						it.append("company_id", req.getCompanyID() )
						if(ids.first() != "*") it.append("_id", D("\$in", ids))
					}

					it
				})
				.sort(D(sortField, sortValue))
				.skip(skip)
				.limit(limit)
				.projection(D(fieldList.associateBy({it}, {1}))
					.let {
						it.append("_id", D("\$toString", "\$_id"))
						if(it.containsKey("created_date")) it.append("created_date", D("\$dateToString", D("date", "\$created_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						if(it.containsKey("updated_date")) it.append("updated_date", D("\$dateToString", D("date", "\$updated_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						it
					}
				)
			)
			.collectList()
			.map {
//				println(it)
				Status.status200Ok(it)
			}
	} //  end of get()

	fun post(req: SafeRequest): Mono<Map<String,Any>> {
		return Mono.just(mapOf())
	} // end of postAndPut()

}
