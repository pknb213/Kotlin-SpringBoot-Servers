package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorSessionByCountry
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime

/**
 * indicator_session_by_country 컬렉션
 * 세션 수 (국가별)
 * sco
 * @author LeeJaeun
 * TODO 기존 anejo 에서는 국가정보 db에서 가져옴. 정책 정해지면 수정할 것.
 */
object SessionByCountryBatch {

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
		"ai" : "#ai",
		"av" : "#av",
		"dl" : "#dl",
		"st": {"#toDate":{"#concat":[
			{"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
			"0:00"
		]}}
	},
    "sco": {"#sum":1}
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
		"dl": "#_id.dl",
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
    "into": "${IndicatorSessionByCountry.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "dl"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "dl" : "#dl",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "sco" : {"#sum" : ["#sco" , "##new.sco"]}}
      }
    ],
    "whenNotMatched": "insert" }}
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
			"dl" : "#dl"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
		"dl": "#_id.dl"
  }]}},
	{"#lookup":{
    "from": "${IndicatorSessionByCountry.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "dl" : "#dl"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "#stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
						{ "#eq": [ "#dl",  "##dl" ] }
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
			"dl":"#data.dl",
			"stz": "#data.stzd"
    },
		"sco":{"#sum":"#data.sco"}
  }},
	
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "av": "#_id.av",
		"dl": "#_id.dl",
    "stz": "#_id.stz",
		"sco": "#sco"
  }]}},
  {"#merge": {
    "into": "${IndicatorSessionByCountry.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "dl"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]			
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorSessionByCountry.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}