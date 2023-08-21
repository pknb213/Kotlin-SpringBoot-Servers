package io.userhabit.polaris.service

import com.mongodb.client.model.DeleteOneModel
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.util.*
import org.bson.Document as D


/**
 * @author sbnoh
 * @comment 21.11.22 yj
 * @sample [GET {{localhost}}/v3/company/{ids}]
 * @return data=[...]
 * {
	"manager_id": "test01@userhabit.io",
	"plan_status": "사용중",
	"created_date": "2021-05-01T12:00:00Z",
	"_id": "company_name_1"
 * }, ...
 */
object CompanyService {
	private val log = Loggers.getLogger(this.javaClass)
	const val COLLECTION_NAME = "company"
	// @deprecated
	// lateinit var FIELD_LIST: List<Map<String, String>>

	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val ids = req.splitParamOrDefault("ids",)
		val fieldList = req.splitQueryOrDefault("field_list", "manager_id,plan_status,created_date")
		val sortField = req.getQueryOrDefault("sort_field", "created_date")
		val sortValue = req.getQueryOrDefault("sort_value", -1)
		val skip = req.getQueryOrDefault("skip",0)
		val limit = req.getQueryOrDefault("limit", 20)
		val searchExpr = req.getQueryOrDefault("search_expr",)

		val validator = Validator()
			.new(limit, "limit").max(100)

		if (validator.isNotValid())
			throw PolarisException.status400BadRequest(validator.toExceptionList())

		return Flux
			.from(MongodbUtil.getCollection(COLLECTION_NAME)
				.find( D().let {
					if(Level.hasPermission(req.getLevel(), Level.SYSTEM)) {
						if(ids.first() != "*") it.append("_id", D("\$in", QueryPeasant.convertToObjectIdList(ids)))
					}else if(ids.first() == "*"){
						it.append("_id", ObjectId(req.getCompanyID()))
					}else{
						val id = req.getCompanyID()
						val notIdList = ids.filter { it != id }
						if(notIdList.isNotEmpty())
							throw PolarisException.status403Forbidden(notIdList.map { mapOf("id[$it]" to "") })

						it.append("_id", D("\$in", QueryPeasant.convertToObjectIdList(ids)))
					}
					if(searchExpr.isNotEmpty()) it.putAll(D.parse(searchExpr))

					it
				})
				.sort(D(sortField, sortValue))
				.skip(skip)
				.limit(limit)
				.projection( fieldList
					.fold(D()){doc, key -> doc.append(key, 1) }
					.let{
						it.append("_id", D("\$toString", "\$_id"))
						if(it.containsKey("created_date")) it.append("created_date", D("\$dateToString", D("date", "\$created_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						if(it.containsKey("updated_date")) it.append("updated_date", D("\$dateToString", D("date", "\$updated_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						if(it.containsKey("payment_date")) it.append("payment_date", D("\$dateToString", D("date", "\$payment_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						if(it.containsKey("plan_begin_date")) it.append("plan_begin_date", D("\$dateToString", D("date", "\$plan_begin_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						if(it.containsKey("plan_end_date")) it.append("plan_end_date", D("\$dateToString", D("date", "\$plan_end_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						if(it.containsKey("plan_request.created_date")) it.append("plan_request.created_date", D("\$dateToString", D("date", "\$plan_request.created_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
						it
					}
				)
			)
			.collectList()
			.map{
				Status.status200Ok(it)
			}
	} // end of get()

	fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
		val level = req.getLevel()
		val isSystem = Level.hasPermission(level, Level.SYSTEM)
		if(!isSystem && !Level.hasPermission(level, Level.ADMINISTRATOR))
			throw PolarisException.status403Forbidden()

		val isPost = req.method() == "POST"
		val bodyList =  try { req.getBodyJsonToList() }catch (e: Exception) { throw PolarisException.status400BadRequest(req.getBody().replace("\n", ""))}

		val vali = Validator()
		bodyList.forEach {
			if(isPost){
				vali.new(it, "app_id").required()
				vali.new(it, "member_id").required()
				vali.new(it, "manager_id").required()
			}else{
				vali.new(it, "id").required()
			}
			if(!Level.contains(level, Level.SYSTEM, Level.ADMINISTRATOR)){
				vali.new(it, "name").ifExistDenied()
			}

			if(!isSystem){
				vali.new(it, "payment_type"   ).ifExistDenied()
				vali.new(it, "payment_status" ).ifExistDenied()
				vali.new(it, "payment_date"   ).ifExistDenied()
			}
			vali.new(it, "payment_type").required(listOf("무통장", "통장", "신용카드"))
			vali.new(it, "payment_status").required(listOf("결제완료", "TODO", "TODO")) // TODO
			vali.new(it, "payment_date"   ).date()

			vali.new(it, "plan_status"    ).ifExistDenied()
			vali.new(it, "plan_type"      ).ifExistDenied()
			vali.new(it, "price"          ).ifExistDenied()
			vali.new(it, "price_off"      ).ifExistDenied()
			vali.new(it, "plan_name"      ).ifExistDenied()
			vali.new(it, "app_limit"      ).ifExistDenied()
			vali.new(it, "session_limit"  ).ifExistDenied()
			vali.new(it, "member_limit"   ).ifExistDenied()
			vali.new(it, "raw_limit"      ).ifExistDenied()
			vali.new(it, "url_limit"      ).ifExistDenied()
			vali.new(it, "reason_del"     ).ifExistDenied()
			vali.new(it, "plan_begin_date").ifExistDenied()
			vali.new(it, "plan_end_date"  ).ifExistDenied()

			it["plan_request"]?.let {
				val m = it as Map<*, *>
				if(m.isNotEmpty()){
					vali.new(m, "plan_status"    ).required(listOf("변경신청", "해지신청"))
					vali.new(m, "plan_type"      )
					vali.new(m, "price"          ).number()
					vali.new(m, "price_off"      ).number()
					vali.new(m, "plan_name"      ).required(listOf("엔터프라이즈", "")) // TODO
					vali.new(m, "app_limit"      ).number()
					vali.new(m, "session_limit"  ).number()
					vali.new(m, "member_limit"   ).number()
					vali.new(m, "raw_limit"      ).number()
					vali.new(m, "url_limit"      ).number()
					vali.new(m, "plan_begin_date").date()
					vali.new(m, "plan_end_date"  ).date()
				}
			}
		}
		vali.new(bodyList, "post body").required()
		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Flux
			.from (
				if(isPost)
					MongodbUtil.getCollection(PlanService.COLLECTION_NAME)
						.find(D("_id", "POC"))
				else
					Mono.empty()
			)
			.collectList()
			.flatMap {
				// TODO it
				val updateModel = bodyList.map {
					val updateDoc = D()
					it["app_id"         ]?.let { updateDoc.append("\$push", D("app_id", it)) }
					it["member_id"        ]?.let { updateDoc.append("\$push", D("member_id", it)) }
					it["manager_id"     ]?.let { updateDoc.append("manager_id"     , it) }
					it["payment_type"   ]?.let { updateDoc.append("payment_type"   , it) }
					it["payment_status" ]?.let {
						updateDoc.append("payment_status" , it)
						if(it == "결제완료") updateDoc.append("payment_date"   , Date())
					}
					it["plan_request"   ]?.let { updateDoc.append("plan_request"   , it) } // TODO it.map{ it as D()} 안해도 되나???
					updateDoc.append("updated_date", Date())
					if (isPost) updateDoc.append("created_date", Date())

					UpdateOneModel<D>(
						D("_id", if(isPost) ObjectId() else ObjectId(it["id"].toString())),
						D("\$set", updateDoc),
						UpdateOptions().upsert(true)
					)
				}

				Mono.from( MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(updateModel) )
			}
			.map {
				Status.status200Ok(it)
			}
	} // end of postAndPut()

	/**
	 * 플랜 변경,해지 신청건 처리(플랜 컬랙션 수정), 시스템 계정만 가능
	 */
	fun putPlan(req: SafeRequest): Mono<Map<String, Any>> {
		val level = req.getLevel()
		if(!Level.hasPermission(level, Level.SYSTEM))
			throw PolarisException.status403Forbidden()

		val ids = req.splitParamOrDefault("ids")

		val vali = Validator()
		vali.new(ids, "ids param").required()
		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

//			"plan_request":{
//			  "plan_status": "변경신청",
//			  "plan_type": "연간",
//			  "price": {"$numberLong": "5000000"},
//			  "price_off": {"$numberLong": "5000000"},
//			  "plan_name":"엔터프라이즈",
//			  "app_limit": 5,
//			  "session_limit":3000,
//			  "member_limit":0,
//			  "raw_limit":0,
//			  "url_limit":7,
//			  "plan_begin_date": {"$date": "2021-03-11T12:00:00Z"},
//			  "plan_end_date": {"$date": "2021-03-11T12:00:00Z"}
//		  },
		// TODO 몽고 트랜잭션을 사용해보자
		return Flux
			.from(
				// TODO aggregate, email 전송 정보 plan 컬랙션에 가져오기
				MongodbUtil.getCollection(COLLECTION_NAME)
					.find(
						D("_id", D("\$in", ids))
					)
					.projection(
						D()
							.append("manager_id", 1)
							.append("manager_name", "\$name")
							.append("plan_name", 1)
							.append("plan_request", 1)
					)
			)
			.collectList()
			.zipWhen {
				Mono
					.from(
						MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(
							it.map {
								UpdateOneModel(
									D("_id", it["_id"]),
									D("\$set", (it["plan_request"] as D)
										.append("plan_status", "사용중")
									),
									UpdateOptions().upsert(true))
							})
					)
			}
			.zipWhen {
				Flux
					.fromIterable(it.t1)
					.flatMap {
						val planStatus = (it["plan_request"] as D)["plan_status"]
						if(planStatus == "변경신청"){
							Email
								.sendPlanChange(req.lang, it["manager_id"].toString(), it["manager_name"].toString(),
									it["plan_name"].toString() , it[""].toString())
						} else if(planStatus == "해지신청"){
							Email
								.sendPlanDelete(req.lang, it["manager_id"].toString(), it["manager_name"].toString())
						} else {
							Mono.just(1)
						}
					}
					.subscribe() // 중요!! 이메일 발송은 비동기로 실행. rx 흐름으로 연결하면 너무 느림

				// TODO match req value
				PlanHistoryService.post(req)
			}
			.map {
				Status.status200Ok(listOf(it.t1.t2, it.t2["data"]))
			}
	} // end of putPlan()

	// TODO 단독으로 삭제 기능이 필요한가? 마지막 남은 user administrator 삭제시 의존적으로 삭제해야 할거 같은데??
	fun delete(req: SafeRequest): Mono<Map<String, Any>> {
		val idList = req.splitParamOrDefault("ids")
		val level = req.getLevel()

		if(Level.hasPermission(level, Level.ADMINISTRATOR)){
//			val notAppIdList = idList.filter { !.contains(it) }
//			if (notAppIdList.isNotEmpty())
//				throw PolarisException.status403Forbidden(notAppIdList.map { mapOf("id[$it]" to "Permission denied") })
		}else if(!Level.hasPermission(level, Level.SYSTEM)){
			throw PolarisException.status403Forbidden()
		}

		val vali = Validator()
		vali.new(idList, "ids param").required()

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return Mono
			.from(
				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME)
					.bulkWrite(idList.map {
						DeleteOneModel( D("_id", ObjectId(it)), )
					}))
			)
			.map {
				Status.status200Ok(it)
			}
	} // end of delete()

}

