package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorAllByView
import io.userhabit.batch.indicators.ViewList
import io.userhabit.polaris.service.SessionService
import org.bson.Document
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.*
import io.userhabit.polaris.EventType as ET

/**
 * 화면 지표
 * 본 indicator_all_by_view 컬렉션의 키 및 인덱스
 * stz, ai, av, vhi
 * 뷰 체류시간의 합
 * @author cjh
 */
object DwellTimeByViewBatch {
  /**
   * 스크린 체류시간의 합 10분
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
            { "#in": [ "#t",  [${ET.VIEW_START}, ${ET.VIEW_END}] ] }
          ]}
        }
      }
    ],
    "as":"event"
  }},
  {"#unwind":"#event"},
  {"#group" : {
    "_id" : {
      "si" : "#si",
      "ai" : "#ai",
      "av" : "#av",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}},
      "vhi" : "#event.ofvhi",
      "uts" : "#event.uts"
    },
    "min_time" : {"#min" : "#event._id.ts"},
    "max_time" : {"#max" : "#event._id.ts"}
  }},
  {"#project" : {
    "dt" : {"#subtract" : ["#max_time", "#min_time"]}
  }},
  {"#group" : {
    "_id" : {
      "st": "#_id.st",
      "ai": "#_id.ai",
      "av": "#_id.av",
      "si" : "#_id.si",
      "vhi": "#_id.vhi"
    },
    "dt" : {"#sum" : "#dt"}
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
    "dt": "#dt"
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
        "dt" : {"#sum" : ["#dt" , "##new.dt"]}
      }}
    ],
    "whenNotMatched": "insert"}}
]
    """
    return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
  }

  /**
   * 뷰 체류시간의 합 1일
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
    "vhi" : "#_id.vhi"
  }]}},
  {"#lookup":{
    "from": "${IndicatorAllByView.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "vhi" : "#vhi"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd"]},
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
    "dt":{"#sum":"#data.dt"}
  }},
  
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "av": "#_id.av",
    "vhi": "#_id.vhi",
    "stz": "#_id.stz",
    "dt":"#dt"
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
