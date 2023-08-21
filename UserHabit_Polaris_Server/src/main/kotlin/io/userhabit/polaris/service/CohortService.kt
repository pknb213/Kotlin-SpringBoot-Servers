package io.userhabit.polaris.service

import com.mongodb.client.model.DeleteOneModel
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.userhabit.common.*
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.util.*
import org.bson.Document as D


object CohortService {
	private val log = Loggers.getLogger(this.javaClass)
	const val COLLECTION_NAME = "cohort"
	// @deprecated
	// lateinit var FIELD_LIST: List<Map<String, String>>

	/**
	 * @author sbnoh
	 */
	fun get(req: SafeRequest): Mono<Map<String, Any>> {
//		val sessionIdList = req.splitQueryOrDefault("session_id_list","")
		val fieldList = req.splitQueryOrDefault("field_list", "")
		val sortField = req.getQueryOrDefault("sort_field", "created_date")
		val sortValue = req.getQueryOrDefault("sort_value", -1)
		val skip = req.getQueryOrDefault("skip",0)
		val limit = req.getQueryOrDefault("limit", 20)
		val searchValue = req.getQueryOrDefault("search_value","")

		val validator = Validator()
			.new(limit, "limit").max(100)

		if (validator.isNotValid())
			throw PolarisException.status400BadRequest(validator.toExceptionList())

		return Mono.just(Status.status200Ok("TODO"))
	}

	/**
	 *  @author sbnoh
	 */
	fun post(req: SafeRequest): Mono<Map<String, Any>> {
		val docList = req.getBodyJsonToList()
		val vali = Validator()
		docList.forEach {
			vali.new(it.getOrDefault("time_zone", "")    as String, "time_zone / ${it.toString()}").required()
			vali.new(it.getOrDefault("name", "")         as String, "name / ${it.toString()}").required()
			vali.new(it.getOrDefault("platform", "")     as String, "platform / ${it.toString()}").required()
			vali.new(it.getOrDefault("session_limit", 0) as Int,    "session_limit / ${it.toString()}").required()
			vali.new(it.getOrDefault("plan_type", "")    as String, "plan_type / ${it.toString()}").required()
		}

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Flux
			.fromIterable(
				docList
			)
			.map { doc ->
				InsertOneModel<D>(
					D(doc)
						.append("created_date", Date())
						.append("exfiry_date", null)
				)
			}
			.collectList()
			.flatMap {
				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(it))
			}
			.map {
				Status.status200Ok(it)
			}
	}

	/**
	 *  @author sbnoh
	 */
	fun put(req: SafeRequest): Mono<Map<String, Any>> {
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
				UpdateOneModel<D>(
					D("_id", ObjectId(doc.remove("_id") as String)),
					D("\$set", doc),
					UpdateOptions().upsert(true))
			}
			.collectList()
			.flatMap {
				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(it))
			}
			.map {
				Status.status200Ok(it)
			}
	}

	/**
	 * @author sbnoh
	 */
	fun deleteFlag(req: SafeRequest): Mono<Map<String, Any>> {

		// 실제 삭제하지 않고 오늘 + 30일 을  expiry_date 필드에 업데이트 한다.
		// 그리고 스케쥴러에서 30일이 되면 실제 삭제한다.

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
				DeleteOneModel<D>( D("_id", ObjectId(doc.remove("_id") as String)), )
			}
			.collectList()
			.flatMap {
				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(it))
			}
			.map {
				Status.status200Ok(it)
			}
	}

	fun delete(req: SafeRequest): Mono<Map<String, Any>> {
		// 스케쥴러에 의해 실제 삭제
		// 이 함수는 라우터에 등록하지 않는다.

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
				DeleteOneModel<D>( D("_id", ObjectId(doc.remove("_id") as String)), )
			}
			.collectList()
			.flatMap {
				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(it))
			}
			.map {
				Status.status200Ok(it)
			}
	}

}

