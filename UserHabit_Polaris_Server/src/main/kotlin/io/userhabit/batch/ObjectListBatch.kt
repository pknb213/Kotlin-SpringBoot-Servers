package io.userhabit.batch

import io.userhabit.batch.indicators.ObjectList
import io.userhabit.common.utils.QueryPeasant
import io.userhabit.polaris.EventType as ET
import io.userhabit.polaris.service.SessionService
import io.userhabit.polaris.service.StorageService
import org.bson.Document
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.*

/**
 * objectList : [ 오브젝트 리스트 배치 : 정적 오브젝트 리스트 생성 배치 ]
 * 배치 : 10분 배치, 1일 배치
 * 내용 : 1일 배치 시 overwrite 하지 않음. 이미 저장 된 값 유지(keepExisting)
 * @author cjh
 */

object ObjectListBatch {

	fun tenMinutes(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
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
            { "#eq": [ "#t",  ${ET.REACT_TAP} ] },
						{ "#gt": [ "#oi", null ] }
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
      "vhi":"#event.ofvhi",
      "oi":"#event.oi",
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
    "vhi": "#_id.vhi",
    "av": "#_id.av",
    "oi": "#_id.oi",
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
    "into": "${ObjectList.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi", "oi"],
    "whenMatched": "merge",
    "whenNotMatched": "insert"}}
]
        """
		return BatchUtil.run(object : Any() {}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}


	fun oneDay(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
		val fullPath = QueryPeasant.getImageURI()
		val query = listOf(
			Document("\$match", mapOf("bft" to mapOf(
				"\$gte" to Date.from(fromDt.toInstant()),
				"\$lt" to Date.from(toDt.toInstant())
//				"\$gte" to mapOf("\$date" to fromDt.format(BatchUtil.toDateFormater)),
//				"\$lt" to mapOf("\$date" to toDt.format(BatchUtil.toDateFormater))
			))),
			Document("\$group", mapOf(
				"_id" to mapOf(
					"stzd" to "\$stzd",
					"ai" to "\$ai",
					"av" to "\$av",
					"vhi" to "\$vhi",
					"oi" to "\$oi"
				)
			)),
			Document("\$replaceWith", mapOf("\$mergeObjects" to mapOf(
				"stzd" to "\$_id.stzd",
				"ai" to "\$_id.ai",
				"av" to "\$_id.av",
				"vhi" to "\$_id.vhi",
				"oi" to "\$_id.oi"
			))),
			Document("\$lookup", mapOf(
				"from" to ObjectList.COLLECTION_NAME_PT10M,
				"let" to mapOf("stzd" to "\$stzd", "ai" to "\$ai", "av" to "\$av", "vhi" to "\$vhi", "oi" to "\$oi"),
				"pipeline" to  listOf(
					mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
						mapOf("\$eq" to listOf("\$stzd", "\$\$stzd")),
						mapOf("\$eq" to listOf("\$ai", "\$\$ai")),
						mapOf("\$eq" to listOf("\$av", "\$\$av")),
						mapOf("\$eq" to listOf("\$vhi", "\$\$vhi")),
						mapOf("\$eq" to listOf("\$oi", "\$\$oi"))
					))))
				),
				"as" to "data"
			)),
			Document("\$unwind", "\$data"),
			Document("\$group", mapOf(
				"_id" to mapOf(
					"ai" to "\$data.ai",
					"av" to "\$data.av",
					"oi" to "\$data.oi",
					"vhi" to "\$data.vhi",
					"stz" to "\$data.stzd"
				)
			)),
			Document("\$lookup", mapOf(
				"from" to StorageService.COLLECTION_NAME,
				"let" to mapOf("ai" to "\$_id.ai", "av" to "\$_id.av", "oi" to "\$_id.oi"),
				"pipeline" to listOf(
					mapOf("\$match" to mapOf("\$expr" to mapOf("\$and" to listOf(
						mapOf("\$eq" to listOf("\$ai", "\$\$ai")),
						mapOf("\$eq" to listOf("\$av", "\$\$av")),
						mapOf("\$eq" to listOf("\$oi", "\$\$oi"))
					))))
				),
				"as" to "storage"
			)),
			Document("\$unwind", mapOf(
				"path" to "\$storage",
				"preserveNullAndEmptyArrays" to true
			)),
			Document("\$replaceWith", mapOf(
				"\$mergeObjects" to listOf(mapOf(
					"ai" to "\$_id.ai",
					"av" to "\$_id.av",
					"oi" to "\$_id.oi",
					"vhi" to "\$_id.vhi",
					"path" to fullPath,
				))
			)),
			Document("\$sort", mapOf("av" to 1)),
			Document(mapOf("\$merge" to mapOf(
				"into" to ObjectList.COLLECTION_NAME_PT24H,
				"on" to listOf("ai", "av", "vhi", "oi"),
				"whenMatched" to "keepExisting",
				"whenNotMatched" to "insert"
			)))
		)
//		return Flux.from(MongodbUtil.getCollection(ObjectList.COLLECTION_NAME_PT10M).aggregate(
//			query
//		)).collectList().map { println("->> ${Util.toPretty(it)}")
//			Util.toPretty(it) }

		val queryStr = "[" + query.joinToString(separator = ",", transform = {
			it.toJson()
		}) + "]"

		return BatchUtil.run(object : Any(){}.javaClass, queryStr, ObjectList.COLLECTION_NAME_PT10M, fromDt, toDt)

//		val query2 = """
//[
//  {"#match":{
//    "bft" : {
//			"#gte": {"#date": "${fromDt.format(BatchUtil.toDateFormater)}"},
//      "#lt":  {"#date": "${toDt.format(BatchUtil.toDateFormater)}"}
//    }
//  }},
//	{"#group" : {
//    "_id" : {
//		"stzd" : "#stzd",
//		"ai" : "#ai",
//		"av" : "#av",
//		"vhi" : "#vhi",
//		"oi" : "#oi"
//    }
//  }},
//  {"#replaceWith":{ "#mergeObjects": [ {
//    "stzd": "#_id.stzd",
//    "ai": "#_id.ai",
//    "av": "#_id.av",
//	"vhi": "#_id.vhi",
//	"oi" : "#_id.oi"
//  }]}},
//	{"#lookup":{
//    "from": "${ObjectList.COLLECTION_NAME_PT10M}",
//    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "vhi" : "#vhi", "oi" : "#oi"},
//    "pipeline": [
//      { "#match":
//        { "#expr":
//          { "#and": [
//            { "#eq": [ "#stzd", "##stzd" ]},
//            { "#eq": [ "#ai",  "##ai" ] },
//            { "#eq": [ "#av",  "##av" ] },
//            { "#eq": [ "#vhi",  "##vhi" ] },
//            { "#eq": [ "#oi",  "##oi" ] }
//          ]}
//        }
//      }
//    ],
//    "as":"data"
//  }},
//  {"#unwind": "#data"},
//
//  {"#group":{
//    "_id":{
//      "ai":"#data.ai",
//      "av":"#data.av",
//      "vhi":"#data.vhi",
//      "oi":"#data.oi",
//      "stz": "#data.stzd"
//    }
//  }},
//  {"#lookup": {
//  	"from": "${StorageService.COLLECTION_NAME}",
//  	"let": { "ai": "#_id.ai", "av": "#_id.av", "oi": "#_id.oi"},
//  	"pipeline": [
//      { "#match":
//        { "#expr":
//          { "#and": [
//            { "#eq": [ "#_id.ai",  "##ai" ] },
//            { "#eq": [ "#_id.av",  "##av" ] },
//            { "#eq": [ "#_id.oi",  "##oi" ] }
//          ]}
//        }
//      }
//    ],
//    "as":"storage"
//  }},
//  {"#unwind": {
//  	"path": "#storage",
//  	"preserveNullAndEmptyArrays": true
//  }},
//  {"#replaceWith":{ "#mergeObjects": [ {
//    "ai": "#_id.ai",
//    "av": "#_id.av",
//    "vhi": "#_id.vhi",
//    "oi": "#_id.oi",
//	"path": $fullPath
//  }]}},
//  {"#merge": {
//    "into": "${ObjectList.COLLECTION_NAME_PT24H}",
//    "on": ["ai", "av", "vhi", "oi"],
//    "whenMatched": "keepExisting",
//    "whenNotMatched": "insert" }}
//]
//        """
//		return BatchUtil.run(object : Any() {}.javaClass, query2, ObjectList.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}
