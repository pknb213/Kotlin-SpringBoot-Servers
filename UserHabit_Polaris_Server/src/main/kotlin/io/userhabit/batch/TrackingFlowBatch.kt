package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorByTrackingFlow
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import io.userhabit.polaris.EventType as ET
import java.time.ZonedDateTime

/**
 * 개별사용흐름, 오브젝트, 이벤트 추적
 * 본 indicator_by_tracking_flow 컬렉션의 키 및 인덱스
 * stz, ai, av, vhi, bvhi, avhi
 * bvhi : 이전 뷰 해시 아이디
 * vhi : 현재 뷰 해시 아이디
 * avhi : 다음 뷰 해시 아이디
 * mco : 이동 카운트
 * t : 이동하기 전 이벤트 타입
 * oi : 이동하기 전 오브젝트 ID
 * TODO 이벤트 타입 확인 할 것, 세션 시작, 세션 종료 vhi 변경 여부
 * 현재는 ###SESSION_START### = 74180013
 * 현재는 ###SESSION_END### = -1671032428
 * @author cjh
 */
object TrackingFlowBatch {
	/**
	 * 개별사용흐름, 오브젝트, 이벤트 추적 10분
	 * @author cjh
	 */

	const val SESSION_START_HASH_ID = 74180013
	const val SESSION_END_HASH_ID = -1671032428

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
            { "#not": { "#in" : ["#t" , [${ET.CRASH}, ${ET.VIEW_START}, ${ET.VIEW_END}, ${ET.APP_START}, ${ET.APP_END}]]} }
          ]}
        }
      }
    ],
    "as":"flow"
  }},

  {"#addFields" : {
    "flow" : {
      "#map" : {
        "input" : "#flow",
        "as" : "row",
        "in" : {
          "bvhi" : {"#cond" : [
	            {"#eq" : [{"#add" : [{ "#indexOfArray" : [ "#flow", "##row"]}, -1]}, -1 ]},
	            ${SESSION_START_HASH_ID},
	            {"#arrayElemAt" : ["#flow.vhi", {"#add" : [{ "#indexOfArray" : [ "#flow", "##row"]}, -1]} ]}
	          ]},
	        "vhi" : {"#arrayElemAt" : ["#flow.vhi", { "#indexOfArray" : [ "#flow", "##row"]} ]},
	        "avhi" : {"#cond" : [
	            {"#eq" : [{"#add" : [{ "#indexOfArray" : [ "#flow", "##row"]}, 1]}, {"#size" : "#flow"} ]},
	            ${SESSION_END_HASH_ID},
	            {"#arrayElemAt" : ["#flow.vhi", {"#add" : [{ "#indexOfArray" : [ "#flow", "##row"]}, 1]} ]}
	          ]},
          "oi" : "##row.oi",
          "t" : "##row.t"
	      }
      }
  }}},

  {"#unwind" : {
    "path" : "#flow"
  }},

  {"#match":{
    "#expr":{"#and":[
      { "#ne" : ["#flow.vhi", "#flow.bvhi"] },
			{ "#ne" : ["#flow.vhi", "#flow.avhi"] }
    ]}
  }},

  {"#group" : {
    "_id" : {
      "ai" : "#ai",
      "av" : "#av",
      "bvhi" : "#flow.bvhi",
      "vhi" : "#flow.vhi",
      "avhi" : "#flow.avhi",
      "t" : "#flow.t",
      "oi" : "#flow.oi",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
    "mco" : {"#sum" : 1}
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
    "vhi": "#_id.vhi",
    "avhi": "#_id.avhi",
    "bvhi": "#_id.bvhi",
    "t" : "#_id.t",
    "oi" : "#_id.oi",
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
    "mco": "#mco"
  }]}},

  {"#merge": {
    "into": "${IndicatorByTrackingFlow.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi", "avhi", "bvhi"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
				"vhi" : "#vhi",
        "avhi" : "#avhi",
				"bvhi" : "#bvhi",
				"t" : "#t",
				"oi" : "#oi",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "mco" : {"#sum" : ["#mco" , "##new.mco"]}}
      }
    ],
    "whenNotMatched": "insert"}}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}

	/**
	 * 개별사용흐름, 오브젝트, 이벤트 추적 1일
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
			"bvhi" : "#bvhi",
			"vhi" : "#vhi",
			"avhi" : "#avhi",
			"t" : "#t",
			"oi" : "#oi"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
		"bvhi": "#_id.bvhi",
		"vhi": "#_id.vhi",
		"avhi": "#_id.avhi",
		"t": "#_id.t",
		"oi": "#_id.oi"
  }]}},
	{"#lookup":{
    "from": "${IndicatorByTrackingFlow.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av",
							"bvhi" : "#bvhi", "vhi" : "#vhi", "avhi" : "#avhi",
							"t" : "#t", "oi" : "#oi"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
						{ "#eq": [ "#vhi",  "##vhi" ] },
						{ "#eq": [ "#avhi",  "##avhi" ] },
						{ "#eq": [ "#bvhi",  "##bvhi" ] },
						{ "#eq": [ "#t",  "##t" ] },
						{ "#eq": [ "#oi",  "##oi" ] }
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
      "avhi":"#data.avhi",
			"bvhi":"#data.bvhi",
      "t":"#data.t",
      "oi":"#data.oi",
			"stz": "#data.stzd"
    },
    "mco":{"#sum":"#data.mco"}
  }},
  
  {"#replaceWith":{ "#mergeObjects": [ {
    "stz": "#_id.stz",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "vhi": "#_id.vhi",
    "avhi": "#_id.avhi",
    "bvhi": "#_id.bvhi",
    "t": "#_id.t",
    "oi": "#_id.oi",
    "mco": "#mco"
  }]}},
  {"#merge": {
    "into": "${IndicatorByTrackingFlow.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "vhi", "avhi", "bvhi"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorByTrackingFlow.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}
