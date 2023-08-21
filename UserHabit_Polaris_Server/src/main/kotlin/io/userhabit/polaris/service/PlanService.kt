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
 * @author nsb
 * @comment 21.11.22 yj
 * @sample [GET {{localhost}}/v3/plan/{ids}?field_list=app_limit,consulting,created_date,updated_date,csv_limit,dats,manager,member_limit,message_off,name,price_off,raw_limit,session_limit,name_ko,type,support_months,support_init,support_tech,type,url_day,url_days]
 * @return data=[...]
 * {
	"app_limit": 0,
	"consulting": true,
	"csv_limit": -1,
	"manager": true,
	"member_limit": 0,
	"message_off": "",
	"name": "Enterprise",
	"name_ko": "엔터프라이즈",
	"price_off": 0,
	"raw_limit": -1,
	"session_limit": -1,
	"support_init": "온/오프라인",
	"support_tech": "온/오프라인",
	"type": "년간",
	"url_day": 30,
	"created_date": "2020-01-01T12:00:00Z",
	"updated_date": "2020-01-01T12:00:00Z",
	"_id": "enterprise_yearly"
 * }, ...
 */
object PlanService {
	private val log = Loggers.getLogger(this.javaClass)
	val COLLECTION_NAME = "plan"
	// @deprecated
	// lateinit var FIELD_LIST: List<Map<String, String>>

	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val ids = req.splitParamOrDefault("ids","")

		val sortField = req.getQueryOrDefault("sort_field", "created_date")
		val sortValue = req.getQueryOrDefault("sort_value", -1)
		val skip = req.getQueryOrDefault("skip",0)
		val limit = req.getQueryOrDefault("limit", 20)
		val fieldList = req.splitQueryOrDefault("field_list", )
		val searchExpr = req.getQueryOrDefault("search_expr",)

		val vali = Validator()
			.new(limit, "limit").max(100)
		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Flux
			.from(MongodbUtil.getCollection(COLLECTION_NAME)
				.find( D().let {
					if(searchExpr.isNotEmpty()) it.putAll(D.parse(searchExpr))// 이 로직은 반드시 아래 조건 추가 로직보다 우선되어야 함

					if(ids.first() != "*"){
						it.append("_id", D("\$in", ids.map { ObjectId(it) }) )
					}

					it
				})
				.sort(D(sortField, sortValue))
				.skip(skip)
				.limit(limit)
				.projection(D(fieldList.associateWith{ 1 })
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
				Status.status200Ok(it)
			}
	} //  end of get()

	/**
	 * @author ???
	 * @comment 21.11.22 yj
	 * @sample []
	 * @return data=[...]
	 */
	@Suppress("UNCHECKED_CAST")
	fun postAndPut(req: SafeRequest): Mono<Map<String,Any>> {
		val isPost = req.method() == "POST"
		val level = req.getLevel()
		val behaviorLevel = if(isPost) Level.WRITE else Level.UPDATE

		if(isPost && !Level.hasPermission(level, Level.SYSTEM, behaviorLevel))
			throw PolarisException.status403Forbidden()

		val bodyList =  try { req.getBodyJsonToList() }catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}
		val vali = Validator()

		bodyList.forEach {
			if(!isPost) vali.new(it, "id").required()
			vali.new(it, "name").required()
			vali.new(it, "name_ko").required()
			vali.new(it, "list").required().list()
			// TODO 리스트 익덱스도 출력해야 하는데....
			(it["list"] as? List<Map<String, Any>>)?.forEach { map ->
				vali.new(map, "type").required()
				vali.new(map, "price").required().number()
				vali.new(map, "price_off").required().number()
				vali.new(map, "days").required().number()
			}
			vali.new(it, "app_limit").required().number()
			vali.new(it, "member_limit").required().number()
			vali.new(it, "session_limit").required().number()
			vali.new(it, "csv_limit").required().number()
			vali.new(it, "raw_limit").required().number()
			vali.new(it, "url_day").required().number()
			vali.new(it, "storage_month").required().number()
			vali.new(it, "consulting").required(listOf("true","false"))
			vali.new(it, "manager").required(listOf("true","false"))
			vali.new(it, "support_init").required()
			vali.new(it, "support_tech").required()
		}
		vali.new(bodyList, "body").required()

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		val updateModels = bodyList.map { body ->
			val updateDoc = D()
				.append("name",           body["name"])
				.append("name_ko",        body["name_ko"])
				.append("list",           (body["list"] as List<Map<String, Any>>)
					.map { mapOf("type" to it["type"], "price" to it["price"],
						"price_off" to it["price_off"], "message_off" to it["message_off"],
						"days" to it["days"], ) })
				.append("app_limit",      body["app_limit"])
				.append("member_limit",		body["member_limit"])
				.append("session_limit",	body["session_limit"])
				.append("csv_limit",			body["csv_limit"])
				.append("raw_limit",			body["raw_limit"])
				.append("url_day",				body["url_day"])
				.append("storage_month",	body["storage_month"])
				.append("consulting",			body["consulting"])
				.append("manager",				body["manager"])
				.append("support_init",		body["support_init"])
				.append("support_tech",		body["support_tech"])
				.append("updated_date",		Date())
			if(isPost) updateDoc.append("created_date", Date())

			UpdateOneModel<D>(
				D("_id", if(isPost) ObjectId() else body["id"]),
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

	fun delete(req: SafeRequest): Mono<Map<String,Any>> {
		if(!Level.hasPermission(req.getLevel(), Level.SYSTEM, Level.DELETE))
			throw PolarisException.status403Forbidden()

		val planIdList = req.splitParamOrDefault("ids")

		val vali = Validator()
		vali.new(planIdList, "ids param").required()

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Mono
			.from (
				MongodbUtil.getCollection(COLLECTION_NAME).deleteMany(
					D("_id", D("\$in", planIdList))
				)
			)
			.map {
				Status.status200Ok(it)
			}
	} // end of delete()

}
