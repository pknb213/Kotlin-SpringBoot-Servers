package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorObjectByView
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import io.userhabit.polaris.EventType as ET

/**
 * 오브젝트 지표
 * 본 indicator_object_by_view 컬렉션의 키 및 인덱스
 * stz, ai, av, vhi, oi
 * 뷰의 오브젝트 수 합 = ObjectCount = oco
 * @author cjh
 */
object ObjectByViewBatch {
  /**
   * 뷰의 오브젝트 수 합 10분
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
    "from": "event",
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
    },
    "oco" : {"#sum" : 1}
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
    "bft": {"#toDate": "${fromDt.format(BatchUtil.toDateFormater)}"},
    "oco" :"#oco"
  }]}},
  {"#merge": {
    "into": "${IndicatorObjectByView.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi", "oi"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "vhi" : "#vhi",
				"oi" : "#oi",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "oco" : {"#sum" : ["#oco" , "##new.oco"]}}
      }
    ],
    "whenNotMatched": "insert"}}
]
    """
    return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
  }

  /**
   * 뷰의 오브젝트 수 합 1일
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
			"vhi" : "#vhi",
			"oi" : "#oi"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
		"vhi": "#_id.vhi",
		"oi" : "#_id.oi"
  }]}},
  {"#lookup":{
    "from": "${IndicatorObjectByView.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "vhi" : "#vhi", "oi" : "#oi"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
						{ "#eq": [ "#vhi",  "##vhi" ] },
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
      "oi":"#data.oi",
			"stz": "#data.stzd"
    },
    "oco":{"#sum":"#data.oco"}
  }},
  
  {"#replaceWith":{ "#mergeObjects": [ {
    "stz": "#_id.stz",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "vhi": "#_id.vhi",
    "oi": "#_id.oi",
    "oco": "#oco"
  }]}},
  {"#merge": {
    "into": "${IndicatorObjectByView.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "vhi", "oi"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
    return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorObjectByView.COLLECTION_NAME_PT10M, fromDt, toDt)
  }

}
