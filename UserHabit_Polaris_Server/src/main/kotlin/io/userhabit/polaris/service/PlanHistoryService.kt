package io.userhabit.polaris.service

import com.mongodb.client.model.UpdateOneModel
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
 * @sample [GET {{localhost}}/v3/plan_history/?field_list=app_amount,app_id,created_date,member_amount,member_id,plan_id,price,price_type,session_amount,status]
 * @return data=[...]
 * {
	"app_amount": 0,
	"app_id": "e6c101f020e1018b5ba17cdbe32ade2d679b44bc",
	"member_amount": 0,
	"member_id": "test01@userhabit.io",
	"plan_id": "business",
	"price": 1000000,
	"price_type": "연간",
	"session_amount": 0,
	"status": "delete",
	"created_date": "2020-01-01T12:00:00Z",
	"_id": "600e77b325c84421fe528b28"
 * },
 */
object PlanHistoryService {
	private val log = Loggers.getLogger(this.javaClass)
	val COLLECTION_NAME = "plan_history"
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
				Status.status200Ok(it)
			}
	} //  end of get()

	fun post(req: SafeRequest): Mono<Map<String,Any>> {
		val bodyList =  try { req.getBodyJsonToList() }catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}
		val vali = Validator()

		bodyList.forEach {
			vali.new(it, "app_id").required()
			vali.new(it, "plan_id").required()
			vali.new(it, "member_id").required()
			vali.new(it, "price").required().number()
			vali.new(it, "price_type").required(listOf("TODO", "TODO")) // TODO
			vali.new(it, "session_amount").required().number()
			vali.new(it, "app_amount").required().number()
			vali.new(it, "member_amount").required().number()
		}
		vali.new(bodyList, "body").required()

		return Flux
			.fromIterable(
				bodyList
			)
			.map { body ->
				val updateDoc = D()
					.append("app_id"           , body["app_id"           ])
					.append("plan_id"          , body["plan_id"          ])
					.append("member_id"          , body["member_id"          ])
					.append("status"           ,
						if(req.method() == "POST") "create"
						else if(req.method() == "PUT") "update"
						else if(req.method() == "DELETE") "delete"
						else "other"
					)
					.append("price"            , body["price"            ])
					.append("session_amount"   , body["session_amount"   ])
					.append("app_amount"       , body["app_amount"       ])
					.append("member_amount"    , body["member_amount"    ])
					.append("created_date", Date())

				UpdateOneModel<D>(
					D("_id", ObjectId()),
					D("\$set", updateDoc),
//					UpdateOptions().upsert(true)
				)
			}
			.collectList()
			.flatMap {
				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(it))
			}
			.map {
				Status.status200Ok(it)
			}
	} // end of postAndPut()

}
