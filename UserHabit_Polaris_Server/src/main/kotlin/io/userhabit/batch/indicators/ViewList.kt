package io.userhabit.batch.indicators

import io.userhabit.batch.BatchUtil
import io.userhabit.common.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import org.bson.Document as D

object ViewList {
	val COLLECTION_NAME_PT10M = BatchUtil.getNameTenMinutes(this.javaClass)
	val COLLECTION_NAME_PT1H = BatchUtil.getNameOneHour(this.javaClass)
	val COLLECTION_NAME_PT24H = BatchUtil.getNameOneDay(this.javaClass)
	private val log = Loggers.getLogger(this.javaClass)

	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val searchKey= req.getQueryOrDefault("search_key")
		val searchList= req.splitQueryOrDefault("search_list")
		val fieldList = req.splitQueryOrDefault("field_list")
			.fold(D(), { doc, key -> doc.append(key, 1) })
		val limit = req.getQueryOrDefault("limit", 40)

		val vali = Validator()
//			.new(searchList, "search_list").jsonObject()

		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		if(searchList.isEmpty()){
			return Mono.just(mapOf())
		}else{

			val query = listOf(
				D("\$match", D("\$expr", D("\$and", D("\$in", listOf("\$${searchKey}", searchList))))),
				D("\$limit", limit),
				D("\$project", D("_id", 0) + fieldList
				),
			)

			return Flux
				.from(MongodbUtil.getCollection(COLLECTION_NAME_PT24H)
					.aggregate(query))
				.collectList()
				.doOnError {
					log.info(Util.toPretty(query))
				}
				.map {
					Status.status200Ok(it)
				}

		}
	}

}