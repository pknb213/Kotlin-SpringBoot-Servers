package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorByFlow
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import io.userhabit.polaris.EventType as ET


/**
 * 전체 흐름
 * 본 indicator_by_flow 컬렉션의 키 및 인덱스
 * stz, ai, av
 * 해당 흐름 카운트 = fco
 * @author cjh
 */

object AllFlowBatch {
	/**
	 * 전체 사용 흐름 10분
	 * @author cjh
	 */
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
            { "#in": [ "#t",  [${ET.VIEW_START}, ${ET.APP_START}, ${ET.APP_END}] ] }
          ]}
        }
      }
    ],
    "as":"flow"
  }},
  {"#addFields" : {
    "flow" : {
      "#reduce" : {
        "input" : "#flow",
        "initialValue" : ",",
        "in" : { "#concat" : [{"#toString" : "##value"}, {"#toString" : "##this.vhi"}, ","]}
      }
    }
  }},
  {"#group" : {
    "_id" : {
      "ai":"#ai",
			"av":"#av",
			"flow":"#flow",
			"st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
    "fco" : {"#sum" : 1}
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
    "flow": "#_id.flow",
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
	"bft": {"#toDate": "${fromDt.format(BatchUtil.toDateFormater)}"},
	"fco": "#fco"
  }]}},
  {"#merge": {
    "into": "${IndicatorByFlow.COLLECTION_NAME_PT10M}",
    "on": ["st" , "ai" , "av", "flow"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
				"flow" : "#flow",
        "stz" : "##new.stz",
				"stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "fco" : {"#sum" : ["#fco" , "##new.fco"]}}
      }
    ],
    "whenNotMatched": "insert"}}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}

	/**
	 * 전체 사용 흐름 1일
	 * @author cjh
	 */
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
      "flow" : "#flow"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "flow" : "#_id.flow"
  }]}},
	{"#lookup":{
    "from": "${IndicatorByFlow.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "flow" : "#flow"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd"]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
			{ "#eq": [ "#flow",  "##flow" ] }
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
		"flow":"#data.flow",
		"stz": "#data.stzd"
    },
    "fco":{"#sum":"#data.fco"}
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stz": "#_id.stz",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "flow": "#_id.flow",
    "fco": "#fco"
  }]}},
  {"#merge": {
  "into": "${IndicatorByFlow.COLLECTION_NAME_PT24H}",
  "on": ["stz", "ai", "av", "flow"],
  "whenMatched": "merge",
  "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorByFlow.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}