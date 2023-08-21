package io.userhabit.batch

import io.userhabit.batch.indicators.ViewList
import io.userhabit.common.Config
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Status
import io.userhabit.common.Util
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.service.SessionService
import io.userhabit.polaris.service.StorageService
import org.bson.Document
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import io.userhabit.polaris.EventType as ET

/**
 * viewList : [ 뷰 리스트 배치 : 정적 뷰 정보 생성 배치 ]
 * 배치 : 10분 배치, 1일 배치
 * 내용 : 1일 배치 시 overwrite 하지 않음. 이미 저장 된 값 유지(keepExisting)
 * @author cjh
 */

object ViewListBatch {
	private val s3 = Config.get("s3.region")
	fun tenMinutes(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
		val query = listOf(
			Document(
				"\$match", mapOf("\$or" to listOf(
					mapOf("i" to mapOf(
						"\$gte" to Date.from(fromDt.toInstant()),
						"\$lt" to Date.from(toDt.toInstant())
					)),
					mapOf("\$and" to listOf(
						mapOf("i" to mapOf("\$exists" to false)),
						mapOf("st" to mapOf(
							"\$gte" to Date.from(fromDt.minusDays(1).toInstant()),
							"\$lt" to Date.from(toDt.minusDays(1).toInstant())
						))
					))
				))
			),
			Document(
				"\$lookup", mapOf(
					"from" to "event",
					"let" to mapOf("si" to "\$_id"),
					"pipeline" to listOf(
						mapOf(
							"\$match" to mapOf(
								"\$expr" to mapOf(
									"\$and" to listOf(
										mapOf("\$eq" to listOf("\$_id.si", "\$\$si")),
										mapOf("\$in" to listOf("\$t", listOf(ET.VIEW_START, ET.APP_START, ET.APP_END)))
									)
								)
							)
						)
					),
					"as" to "event"
				)
			),
			Document("\$unwind", "\$event"),
			Document(
				"\$group", mapOf(
					"_id" to mapOf(
						"ai" to "\$ai",
						"av" to "\$av",
						"vhi" to "\$event.vhi",
						"vi" to "\$event.vi",
						"st" to mapOf(
							"\$toDate" to mapOf(
								"\$concat" to listOf(
									mapOf(
										"\$substr" to listOf(
											mapOf(
												"\$dateToString" to mapOf(
													"format" to "%Y-%m-%d %H:%M",
													"date" to "\$st"
												)
											),
											0, 15
										)
									),
									"0:00"
								)
							)
						)
					)
				)
			),
			Document(
				"\$addFields", mapOf(
					"_id.ai" to mapOf("\$toObjectId" to "\$_id.ai")
				)
			),
			Document(
				"\$lookup", mapOf(
					"from" to "app",
					"localField" to "_id.ai",
					"foreignField" to "_id",
					"as" to "app"
				)
			),
			Document(
				"\$replaceWith", mapOf(
					"\$mergeObjects" to listOf(
						mapOf(
							"ai" to "\$_id.ai",
							"av" to "\$_id.av",
							"st" to "\$_id.st",
							"vi" to "\$_id.vi",
							"vhi" to "\$_id.vhi",
							"stz" to mapOf(
								"\$toDate" to mapOf(
									"\$dateToString" to mapOf(
										"date" to "\$_id.st",
										"timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
									)
								)
							),
							"stzd" to mapOf(
								"\$toDate" to mapOf(
									"\$dateToString" to mapOf(
										"format" to "%Y-%m-%d 00:00:00",
										"date" to "\$_id.st",
										"timezone" to mapOf("\$arrayElemAt" to listOf("\$app.time_zone", 0))
									)
								)
							),
							"bft" to mapOf("\$toDate" to fromDt.format(BatchUtil.toDateFormater)),
						)
					)
				)
			),
			Document(
				"\$merge", mapOf(
					"into" to ViewList.COLLECTION_NAME_PT10M,
					"on" to listOf("st", "ai", "av", "vhi"),
					"whenMatched" to "merge",
					"whenNotMatched" to "insert"
				)
			)
		)
//		return Flux.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).aggregate(
//			query
//		).allowDiskUse(true).explain()).collectList().map {
//			println(">> ${fromDt.minusDays(1)}, ${toDt.plusHours(3)}")
//			println(">> ${Util.toPretty(it)}")
//			println(">> ${it.count()}")
//			it.toString() }

		val queryStr = "[" + query.joinToString(separator = ",", transform = {
			it.toJson()
		}) + "]"

		return BatchUtil.run(object : Any() {}.javaClass, queryStr, SessionService.COLLECTION_NAME, fromDt, toDt)
	}

	fun oneDay(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
//		println(">> $fromDt, ${toDt.plusDays(1)}")
		val fullPath = QueryPeasant.getImageURI()
		val query = listOf(
			Document(
				mapOf(
					"\$match" to mapOf(
						"bft" to mapOf(
							"\$gte" to Date.from(fromDt.toInstant()),
							"\$lt" to Date.from(toDt.toInstant())
						)
					)
				)
			),
			Document(
				mapOf(
					"\$group" to mapOf(
						"_id" to mapOf(
							"stzd" to "\$stzd",
							"ai" to "\$ai",
							"av" to "\$av",
							"vhi" to "\$vhi"
						)
					)
				)
			),
			Document(
				mapOf(
					"\$replaceWith" to mapOf(
						"\$mergeObjects" to listOf(
							mapOf(
								"stzd" to "\$_id.stzd",
								"ai" to "\$_id.ai",
								"av" to "\$_id.av",
								"vhi" to "\$_id.vhi"
							)
						)
					)
				)
			),
			Document(
				mapOf(
					"\$lookup" to mapOf(
						"from" to ViewList.COLLECTION_NAME_PT10M,
						"let" to mapOf("stzd" to "\$stzd", "ai" to "\$ai", "av" to "\$av", "vhi" to "\$vhi"),
						"pipeline" to listOf(
							mapOf(
								"\$match" to mapOf(
									"\$expr" to mapOf(
										"\$and" to listOf(
											mapOf("\$eq" to listOf("\$stzd", "\$\$stzd")),
											mapOf("\$eq" to listOf("\$ai", "\$\$ai")),
											mapOf("\$eq" to listOf("\$av", "\$\$av")),
											mapOf("\$eq" to listOf("\$vhi", "\$\$vhi"))
										)
									)
								)
							)
						),
						"as" to "data"
					)
				)
			),
			Document(mapOf("\$unwind" to "\$data")),
			Document(
				mapOf(
					"\$group" to mapOf(
						"_id" to mapOf(
							"ai" to "\$data.ai",
							"av" to "\$data.av",
							"vi" to "\$data.vi",
							"vhi" to "\$data.vhi",
							"stz" to "\$data.stzd"
						)
					)
				)
			),
			Document(
				mapOf(
					"\$lookup" to mapOf(
						"from" to StorageService.COLLECTION_NAME,
						"let" to mapOf("ai" to "\$_id.ai", "av" to "\$_id.av", "vi" to "\$_id.vi"),
						"pipeline" to listOf(
							mapOf(
								"\$match" to mapOf(
									"\$expr" to mapOf(
										"\$and" to listOf(
											mapOf("\$eq" to listOf("\$ai", "\$\$ai")),
											mapOf("\$eq" to listOf("\$av", "\$\$av")),
											mapOf("\$eq" to listOf("\$vi", "\$\$vi")),
											mapOf("\$eq" to listOf("\$oi", null)),
											mapOf("\$eq" to listOf("\$svi", null))
										)
									)
								)
							)
						),
						"as" to "storage"
					)
				)
			),
			Document(
				mapOf(
					"\$unwind" to mapOf(
						"path" to "\$storage",
						"preserveNullAndEmptyArrays" to true
					)
				)
			),
			/**
			 * S3 URL: s3://service-uh-polaris-stage-2/attach/b3068e50a8afca37a2909990f9b8c0f7efbe2168/1.0.1/-1044417443
			 * 객체 URl: https://service-uh-polaris-stage-2.s3.ap-northeast-2.amazonaws.com/attach/b3068e50a8afca37a2909990f9b8c0f7efbe2168/1.0.1/-1044417443
			 * GET storage: path": "s3:/b3068e50a8afca37a2909990f9b8c0f7efbe2168/1.0.1/530167440
			 */
			Document(
				mapOf(
					"\$replaceWith" to mapOf(
						"\$mergeObjects" to listOf(
							mapOf(
								"ai" to "\$_id.ai",
								"av" to "\$_id.av",
								"vi" to "\$_id.vi",
								"vhi" to "\$_id.vhi",
								"path" to fullPath,
							)
						)
					)
				)
			),
			Document(mapOf("\$sort" to mapOf("av" to 1))),
			Document(
				mapOf(
					"\$merge" to mapOf(
						"into" to ViewList.COLLECTION_NAME_PT24H,
						"on" to listOf("ai", "av", "vhi"),
						"whenMatched" to "keepExisting",
						"whenNotMatched" to "insert"
					)
				)
			)
		)

		val queryStr = "[" + query.joinToString(separator = ",", transform = {
			it.toJson()
		}) + "]"

		return BatchUtil.run(object : Any() {}.javaClass, queryStr, ViewList.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}

/** Past PT10M, PT24H

val query = """
[
  {"#match": {"#or" : [
    {"i": {
			"#gte" : {"#date": "${fromDt.format(BatchUtil.toDateFormater)}"},
	    "#lt" : {"#date": "${toDt.format(BatchUtil.toDateFormater)}"}
	  }},
	  {"#and": [
			{"i": {"#exists" : false}},
			{"st": {
				"#gte" : {"#date": "${fromDt.minusDays(1).format(BatchUtil.toDateFormater)}"},
	      "#lt" : {"#date": "${toDt.minusDays(1).format(BatchUtil.toDateFormater)}"}
	    }}
	  ]}
  ]}},
  {"#lookup":{
    "from":"event",
    "let": { "si": "#_id"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#_id.si",  "##si" ] },
            { "#in": [ "#t",  [${ET.VIEW_START}, ${ET.APP_START}, ${ET.APP_END}] ] }
          ]}
        }
      }
    ],
    "as":"event"
  }},
  {"#unwind":"#event"},
  {"#group":{
    "_id":{
      "ai":"#ai",
      "av":"#av",
      "vi":"#event.vi",
			"vhi":"#event.ofvhi",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    }
  }},
  {"#addFields": {
  "_id.ai": {
  	"#toObjectId": "#_id.ai"
  }
  }},
  {"#lookup": {
    "from": "app",
    "localField": "_id.ai",
    "foreignField": "_id",
    "as": "app"
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "st": "#_id.st",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "vi": "#_id.vi",
	"vhi": "#_id.vhi",
    "stz": { "#toDate" : { "#dateToString" : {
      "date": "#_id.st",
      "timezone" : {"#arrayElemAt":["#app.time_zone", 0]}
    }}},
	"stzd" : {"#toDate": {"#dateToString": {
		"format": "%Y-%m-%d 00:00:00",
		"date": "#_id.st",
		"timezone" : {"#arrayElemAt":["#app.time_zone", 0]}
	}}},
	"bft": {"#toDate": "${fromDt.format(BatchUtil.toDateFormater)}"}
  }]}},
  {"#merge": {
    "into": "${ViewList.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi"],
    "whenMatched": "merge",
    "whenNotMatched": "insert"}}
]
        """
	val query = """
[
  {"#match":{
    "bft" : {
			"#gte": {"#date": "${fromDt.format(BatchUtil.toDateFormater)}"},
      "#lt":  {"#date": "${toDt.format(BatchUtil.toDateFormater)}"}
    }
  }},
	{"#group" : {
    "_id" : {
      "stzd" : "#stzd",
      "ai" : "#ai",
      "av" : "#av",
		"vhi" : "#vhi"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
	"vhi": "#_id.vhi"
  }]}},
  {"#lookup":{
    "from": "${ViewList.COLLECTION_NAME_PT10M}",
    "let": {
	  "stzd": "#stzd",
	  "ai": "#ai",
	  "av": "#av",
	  "vhi": "#vhi"
	},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
			{ "#eq": [ "#vhi",  "##vhi" ] }
          ]}
        }
      }
    ],
    "as": "data"
  }},
	{"#unwind": "#data"},
	{"#group":{
    "_id":{
		"ai":"#data.ai",
		"av":"#data.av",
		"vi":"#data.vi",
		"vhi":"#data.vhi",
		"stz": "#data.stzd"
    }
  }},
  {"#lookup": {
  	"from": "${StorageService.COLLECTION_NAME}",
  	"let": {"ai": "#_id.ai", "av": "#_id.av", "vi": "#_id.vi"},
  	"pipeline": [{
  		"#match": {
			"#expr": { "#and": [
				{ "#eq": [ "#ai",  "##ai" ] },
				{ "#eq": [ "#av",  "##av" ] },
				{ "#eq": [ "#vi",  "##vi" ] }
			]}
  		}
  	}],
  	"as": "storage"
  }},
  {"#unwind": {
  	"path": "#storage",
  	"preserveNullAndEmptyArrays": true
  }},
  {"#addFields" : {
		"path": "#sty/#ai/#av/#fhi",
		"alias" : null,
		"favorite" : false
	}},
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "av": "#_id.av",
	"vi": "#_id.vi",
	"vhi": "#_id.vhi",
	"path": {"#concat": ["#storage.sty", "/", "#storage.ai", "/", "#storage.av", "/", "#storage.fhi"]}
  }]}},
  {"#merge": {
    "into": "${ViewList.COLLECTION_NAME_PT24H}",
    "on": ["ai", "av", "vhi"],
    "whenMatched": "keepExisting",
    "whenNotMatched": "insert" }}
]
        """
*/