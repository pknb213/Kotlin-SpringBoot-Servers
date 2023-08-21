package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorAllByApp
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime


/**
 * indicator_all_by_app 컬렉션
 * 세션수
 * sco
 */
object SessionByAppBatch {

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
  {"#group" : {
    "_id" : {
      "ai":"#ai",
      "av":"#av",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
		},
		"sco" : {"#sum" : 1}
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
    "stz": { "#toDate" : { "#dateToString" : {
      "date": "#_id.st",
      "timezone" : {"#arrayElemAt":["#app.time_zone", 0]}
    }}},
		"stzd" : {"#toDate": {"#dateToString": { 
			"format": "%Y-%m-%d 00:00:00",
			"date": "#_id.st",
			"timezone" : {"#arrayElemAt":["#app.time_zone", 0]}
		}}},
		"bft": {"#toDate": "${fromDt.format(BatchUtil.toDateFormater)}"},
    "sco": "#sco"
  }]}},
  {"#merge": {
    "into": "${IndicatorAllByApp.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "sco" : {"#sum" : ["#sco" , "##new.sco"]}}
      }
    ],
    "whenNotMatched": "insert"}}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}


	fun oneDay(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String>{
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
      "av" : "#av"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av"
  }]}},
	{"#lookup":{
    "from": "${IndicatorAllByApp.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] }
          ]}
        }
      }
    ],
    "as":"data"
  }},
	{"#unwind": "#data"},
	
	{"#group":{
    "_id":{
      "ai":"#data.ai",
			"av":"#data.av",
			"stz": "#data.stzd"
    },
    "sco":{"#sum":"#data.sco"}
  }},
  
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "av": "#_id.av",
    "stz": "#_id.stz",
    "sco": "#sco"
  }]}},
  {"#merge": {
    "into": "${IndicatorAllByApp.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorAllByApp.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}