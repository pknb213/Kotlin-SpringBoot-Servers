package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorReachRateByScrollView
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import io.userhabit.polaris.EventType as ET

/**
 * 스크롤뷰 도달율 배치
 * 유저해빗에서 분석하는 히트맵 디바이스의 비율
 * 16:7 ~ 16:8.5 사이의 디바이스만 분석한다
 * @author cjh
 */
object ReachRateByScrollViewBatch {

	fun tenMinutes(fromDt: ZonedDateTime, toDt: ZonedDateTime) : Mono<String> {
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
            { "#eq": [ "#t", ${ET.SCROLL_CHANGE} ] },
            { "#gte": [ "#spy",  300 ] },
			{ "#gt" : [ "#svi", null ]},
			{ "#ne" : [ "#spx",  -1 ]},
            { "#ne" : [ "#spy",  -1 ]}
          ]}
        }
      }
    ],
    "as":"event"
  }},
  {"#unwind" : "#event"},
  {"#group" : {
    "_id" : {
      "ai" : "#ai",
      "av" : "#av",
      "vhi" : "#event.ofvhi",
      "svi" : "#event.svi",
      "spx" : {"#multiply" : [{"#toInt":{"#divide" : ["#event.spx", 300]}}, 300] },
      "spy" : {"#multiply" : [{"#toInt":{"#divide" : ["#event.spy", 300]}}, 300] },
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
    "count" : {"#sum" : 1}
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
    "ai": "#_id.ai",
    "av": "#_id.av",
    "st": "#_id.st",
    "stz": { "#toDate" : { "#dateToString" : {
	"date": "#_id.st",	
		"timezone" : {"#arrayElemAt":["#app.time_zone", 0]}
    }}},
	"stzd" : {"#toDate": {"#dateToString": { 
		"format": "%Y-%m-%d 00:00:00",
		"date": "#_id.st",
		"timezone" : {"#arrayElemAt":["#app.time_zone", 0]}
	}}},
    "vhi": "#_id.vhi",
    "svi": "#_id.svi",
	"spx": "#_id.spx",
	"spy": "#_id.spy",
	"bft": {"#toDate": "${fromDt.format(BatchUtil.toDateFormater)}"},
	"count": "#count"
  }]}},
  {"#merge": {
    "into": "${IndicatorReachRateByScrollView.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi", "svi", "spx", "spy"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "vhi" : "#vhi",
        "svi" : "#svi",
		"spx" : "#spx",
		"spy" : "#spy",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "count" : {"#sum" : ["#count" , "##new.count"]}}
      }
    ],
    "whenNotMatched": "insert"}}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}

	fun oneDay(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
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
		"vhi" : "#vhi",
		"svi" : "#svi",
		"spx" : "#spx",
		"spy" : "#spy"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
	"vhi": "#_id.vhi",
	"svi": "#_id.svi",
	"spx": "#_id.spx",
	"spy" : "#_id.spy"
  }]}},
	{"#lookup":{
    "from": "${IndicatorReachRateByScrollView.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "vhi" : "#vhi", "svi" : "#svi", "spx" : "#spx", "spy" : "#spy"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
			{ "#eq": [ "#vhi",  "##vhi" ] },
			{ "#eq": [ "#svi",  "##svi" ] },
			{ "#eq": [ "#spx",  "##spx" ] },
			{ "#eq": [ "#spy",  "##spy" ] }
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
		"vhi":"#data.vhi",
		"svi":"#data.svi",
		"spx":"#data.spx",
		"spy":"#data.spy",
		"stz": "#data.stzd"
    },
    "count":{"#sum":"#data.count"}
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "av": "#_id.av",
    "stz": "#_id.stz",
	"vhi": "#_id.vhi",
	"svi": "#_id.svi",
	"spx": "#_id.spx",
	"spy": "#_id.spy",
    "count":"#count"
  }]}},
  {"#merge": {
    "into": "${IndicatorReachRateByScrollView.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "vhi", "svi", "spx", "spy"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorReachRateByScrollView.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}