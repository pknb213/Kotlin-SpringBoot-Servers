package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorSessionByOsDevicename
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime

/**
 * indicator_session_by_os_devicename 컬렉션
 * 세션 수 (OS, 디바이스 별)
 * @author LeeJaeun
 */

object SessionByOsDevicenameBatch {

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
			"dov" : "#dov",
			"dn" : "#dn",
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
		"dov": "#_id.dov",
		"dn": "#_id.dn",
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
    "into": "${IndicatorSessionByOsDevicename.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "dov", "dn"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "dov" : "#dov",
		"dn" : "#dn",
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
			"dov" : "#dov",
			"dn" : "#dn"
    	}
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
	"dov": "#_id.dov",
	"dn" : "#_id.dn"
  }]}},
	{"#lookup":{
    "from": "${IndicatorSessionByOsDevicename.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "dov" : "#dov", "dn" : "#dn"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
			{ "#eq": [ "#dov",  "##dov" ] },
			{ "#eq": [ "#dn",  "##dn" ] }
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
		"dov":"#data.dov",
		"dn":"#data.dn",
		"stz": "#data.stzd"
    },
	"sco":{"#sum":"#data.sco"}
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "av": "#_id.av",
	"dov": "#_id.dov",
	"dn": "#_id.dn",
    "stz": "#_id.stz",
	"sco": "#sco"
  }]}},
  {"#merge": {
    "into": "${IndicatorSessionByOsDevicename.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "dov", "dn"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]			
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorSessionByOsDevicename.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}