package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorHeatmapByScrollView
import io.userhabit.polaris.EventType as ET
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime


/**
 * 스크롤뷰 도달율 배치
 * 유저해빗에서 분석하는 히트맵 디바이스의 비율
 * 16:7 ~ 16:8.5 사이의 디바이스만 분석한다
 * @author cjh
 */
object HeatmapByScrollViewBatch {
	const val REPRESENTWIDTH = 720
	const val REPRESENTHEIGHT = 1280

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
            { "#in": [ "#t",  [${ET.REACT_TAP}, ${ET.REACT_DOUBLE_TAP}, ${ET.REACT_LONG_TAP}, ${ET.REACT_SWIPE}, ${ET.NOACT_TAP}, ${ET.NOACT_DOUBLE_TAP}, ${ET.NOACT_LONG_TAP}, ${ET.NOACT_SWIPE}] ] },
            { "#ne" : [ "#svi", null ]},
			{ "#ne" : [ "#sgx", null ]},
			{ "#ne" : [ "#sgy", null ]},
            { "#gte" : [ "#sgx",  0 ]},
            { "#gte" : [ "#sgy",  0 ]}
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
      "t" : "#event.t",
      "vhi" : "#event.ofvhi",
      "svi" : "#event.svi",
      "spx" : {"#multiply" : [{"#toInt":{"#divide" : [{"#multiply" : ["#event.sgx", {"#divide" : [${REPRESENTWIDTH}, "#dw"]}]}, 50]}}, 50] },
      "spy" : {"#multiply" : [{"#toInt":{"#divide" : [{"#multiply" : ["#event.sgy", {"#divide" : [${REPRESENTHEIGHT}, "#dh"]}]}, 50]}}, 50] },
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
    "count" : {"#sum" : 1}
  }},
  {"#sort" : {"_id.spy" : -1}},
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
    "t": "#_id.t",
    "vhi": "#_id.vhi",
    "svi": "#_id.svi",
	"spx": "#_id.spx",
	"spy": "#_id.spy",
	"bft": {"#toDate": "${fromDt.format(BatchUtil.toDateFormater)}"},
	"count": "#count"
  }]}},
  {"#merge": {
    "into": "${IndicatorHeatmapByScrollView.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi", "t", "svi", "spx", "spy"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "vhi" : "#vhi",
        "svi" : "#svi",
        "t" : "#t",
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
	/** Todo 03.11
	{ "#gt" : [ "#svi", null ]},
	{ "#gt" : [ "#spx", null ]},
	{ "#gt" : [ "#spy", null ]},
	{ "#gte" : [ "#spx",  0 ]},
	{ "#gte" : [ "#spy",  0 ]}
	 and spx, spy -> sgx, sgy
	 * */

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
      "t" : "#t",
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
    "t" : "#_id.t",
		"spx" : "#_id.spx",
		"spy" : "#_id.spy"
  }]}},
	{"#lookup":{
    "from": "${IndicatorHeatmapByScrollView.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "vhi" : "#vhi", "svi" : "#svi", "t" : "#t", "spx" : "#spx", "spy" : "#spy"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
						{ "#eq": [ "#vhi",  "##vhi" ] },
						{ "#eq": [ "#svi",  "##svi" ] },
						{ "#eq": [ "#t",  "##t" ] },
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
			"t":"#data.t",
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
		"t": "#_id.t",
		"spx": "#_id.spx",
		"spy": "#_id.spy",
    "count":"#count"
  }]}},
  {"#merge": {
    "into": "${IndicatorHeatmapByScrollView.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "vhi", "svi", "t", "spx", "spy"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorHeatmapByScrollView.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}