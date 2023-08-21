package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorByEachFlow
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import io.userhabit.polaris.EventType as ET

/**
 * 개별사용흐름
 * 본 indicator_by_each_flow 컬렉션의 키 및 인덱스
 * stz, ai, av, vhi, avhi, bvhi
 * bvhi : 이전 뷰 해시 아이디
 * vhi : 현재 뷰 해시 아이디
 * nvhi : 다음 뷰 해시 아이디
 * mco : 이동 카운트
 * sco : 세션 카운트
 * rco : 반복 카운트
 * ###SESSION_START### = 74180013
 * ###SESSION_END### = -1671032428
 * @author cjh
 */
object EachFlowBatch {
	/**
	 * 개별사용흐름 10분
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
            { "#in" : ["#t" , [${ET.VIEW_START}, ${ET.APP_START}, ${ET.APP_END}]]}
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
	          ]}
	      }
      }
  }}},

  {"#unwind" : {
    "path" : "#flow"
  }},

  {"#match":{
    "#expr":{"#and":[
      { "#ne" : ["#flow.vhi", "#flow.avhi"] }
    ]}
  }},

  {"#group" : {
    "_id" : {
      "si" : "#_id",
      "ai" : "#ai",
      "av" : "#av",
      "bvhi" : "#flow.bvhi",
      "vhi" : "#flow.vhi",
      "avhi" : "#flow.avhi",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
    "mco" : {"#sum" : 1},
    "rco" : {"#sum" : 1}
  }},

  {"#group" : {
    "_id" : {
      "ai" : "#_id.ai",
      "av" : "#_id.av",
      "bvhi" : "#_id.bvhi",
      "vhi" : "#_id.vhi",
      "avhi" : "#_id.avhi",
      "rco" : "#rco",
      "st": "#_id.st"
    },
    "mco" : {"#sum" : "#mco"},
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
    "bvhi": "#_id.bvhi",
    "vhi": "#_id.vhi",
    "avhi": "#_id.avhi",
    "rco" : "#_id.rco",
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
    "mco": "#mco",
    "sco" : "#sco"
  }]}},

  {"#merge": {
    "into": "${IndicatorByEachFlow.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi", "avhi", "bvhi"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
				"bvhi" : "#bvhi",
				"vhi" : "#vhi",
        "avhi" : "#avhi",
				"rco" : "#rco",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "mco" : {"#sum" : ["#mco" , "##new.mco"]},
				"sco" : {"#sum" : ["#sco" , "##new.sco"]}}
      }
    ],
    "whenNotMatched": "insert"}}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}

	/**
	 * 개별사용흐름 1시간
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
      "rco" : "#rco"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "bvhi" : "#_id.bvhi",
    "vhi" : "#_id.vhi",
    "avhi" : "#_id.avhi",
    "rco" : "#_id.rco"
  }]}},
	{"#lookup":{
    "from": "${IndicatorByEachFlow.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "bvhi" : "#bvhi", "vhi" : "#vhi", "avhi" : "#avhi", "rco" : "#rco"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
						{ "#eq": [ "#bvhi",  "##bvhi" ] },
						{ "#eq": [ "#vhi",  "##vhi" ] },
						{ "#eq": [ "#avhi",  "##avhi" ] },
						{ "#eq": [ "#rco",  "##rco" ] }
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
			"bvhi":"#data.bvhi",
      "vhi":"#data.vhi",
      "avhi":"#data.avhi",
      "rco":"#data.rco",
			"stz": "#data.stzd"
    },
    "mco":{"#sum":"#data.mco"},
    "sco":{"#sum":"#data.sco"}
  }},
  
  {"#replaceWith":{ "#mergeObjects": [ {
    "stz": "#_id.stz",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "bvhi": "#_id.bvhi",
    "vhi": "#_id.vhi",
    "avhi": "#_id.avhi",
    "rco" : "#_id.rco",
    "mco": "#mco",
    "sco": "#sco"
  }]}},
  {"#merge": {
    "into": "${IndicatorByEachFlow.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "vhi", "avhi", "bvhi"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorByEachFlow.COLLECTION_NAME_PT10M, fromDt, toDt)
	}

}