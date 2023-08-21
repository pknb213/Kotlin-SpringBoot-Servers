package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorAllByView
import io.userhabit.polaris.EventType
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime

/**
 * 뷰 지표
 * 본 indicator_all_by_view 컬렉션의 키 및 인덱스
 * stz, ai, av, ek
 * 뷰의 스와이프 이벤트 방향 수 합 = swipeDirectionCountOfView
 * @author cjh
 */
object SwipeDirectionByViewBatch {
	/**
	 * 뷰의 스와이프 이벤트 방향 별 수 합 10분
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
            { "#eq": [ "#t", ${EventType.REACT_SWIPE} ] }
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
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
    "dit" : {"#sum" : {"#cond" : [ {"#eq": ["#event.gv", 1]}, 1, 0]}},
    "dib" : {"#sum" : {"#cond" : [ {"#eq": ["#event.gv", 2]}, 1, 0]}},
    "dil" : {"#sum" : {"#cond" : [ {"#eq": ["#event.gv", 3]}, 1, 0]}},
    "dir" : {"#sum" : {"#cond" : [ {"#eq": ["#event.gv", 4]}, 1, 0]}}
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
    "dit" :"#dit",
    "dib" :"#dib",
    "dil" :"#dil",
    "dir" :"#dir"
  }]}},
  {"#merge": {
    "into": "${IndicatorAllByView.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "vhi" : "#vhi",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "dit" : {"#sum" : ["#dit" , "##new.dit"]},
				"dib" : {"#sum" : ["#dib" , "##new.dib"]},
				"dil" : {"#sum" : ["#dil" , "##new.dil"]},
				"dir" : {"#sum" : ["#dir" , "##new.dir"]}}
      }
    ],
    "whenNotMatched": "insert"}}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}

	/**
	 * 뷰의 스와이프 이벤트 방향 별 수 합 1일
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
    "from": "${IndicatorAllByView.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "vhi" : "#vhi"},
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
    "as":"data"
  }},
	{"#unwind": "#data"},
	
	{"#group":{
    "_id":{
      "ai":"#data.ai",
			"av":"#data.av",
      "vhi":"#data.vhi",
			"stz": "#data.stzd"
    },
    "dit":{"#sum":"#data.dit"},
    "dib":{"#sum":"#data.dib"},
    "dir":{"#sum":"#data.dir"},
    "dil":{"#sum":"#data.dil"}
  }},
  
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "av": "#_id.av",
    "vhi": "#_id.vhi",
    "stz": "#_id.stz",
    "dit": "#dit",
    "dib": "#dib",
    "dir": "#dir",
    "dil": "#dil"
  }]}},
  {"#merge": {
    "into": "${IndicatorAllByView.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "vhi"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorAllByView.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}