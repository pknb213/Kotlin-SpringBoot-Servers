package io.userhabit.polaris.dao

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import io.userhabit.common.MongodbUtil
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.util.*
import org.bson.Document as D

/**
 * @author sbnoh
 */
object EmailTemplateDao {
	private val log = Loggers.getLogger(this.javaClass)
	val COLLECTION_NAME = "mail_template"

	val REGISTRATION = "REGISTRATION"
	val AUTHENTICATION = "AUTHENTICATION"
	val CHANGE_PASSWORD = "CHANGE_PASSWORD"
	val HUMAN_ID = "HUMAN_ID"
	val INVITE_NEW_ID = "INVITE_NEW_ID"
	val INVITE_CURRENT_id = "INVITE_CURRENT_id"
	val CHANGE_ADMIN = "CHANGE_ADMIN"
	val DELETE_AUTHORIZATION = "DELETE_AUTHORIZATION"
	val GUIDE_FREEPLAN_20DAY = "GUIDE_FREEPLAN_20DAY"

	fun getTemplate(id: String): Mono<D>{
		return Mono
			.from( MongodbUtil.getCollection(COLLECTION_NAME).find(D("_id", id)) )
	}

	fun createTemplate(id: String, title: String, template: String): Mono<InsertOneResult>{
		val doc = D("_id", id)
			.append("title", title)
			.append("html", template)
			.append("created_date", Date())
			.append("updated_date", template)

		return Mono
			.from( MongodbUtil.getCollection(COLLECTION_NAME).insertOne(doc) )
	}

	fun updateTemplate(id: String, title: String, template: String): Mono<UpdateResult>{
		val doc = D()
			.append("title", title)
			.append("html", template)
			.append("updated_date", template)

		return Mono
			.from( MongodbUtil.getCollection(COLLECTION_NAME).updateOne(D("_id", id), doc) )
	}

	fun deleteTemplate(id: String): Mono<DeleteResult> {

		return Mono
			.from( MongodbUtil.getCollection(COLLECTION_NAME).deleteOne(D("_id", id)) )
	}
}