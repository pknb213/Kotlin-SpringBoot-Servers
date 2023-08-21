package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorSessionDeviceByAppIsCrash
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import io.userhabit.polaris.EventType as ET

object SessionDeviceByAppIsCrashBatch {

	/**
	 * indicator_session_device_by_app_is_crash 컬렉션
	 * 크래시가 발생된 세션& 디바이스 수
	 * csco, cdco
	 * @author lje
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
            { "#in": [ "#t",  [${ET.CRASH}] ] }
          ]}
        }
      }
    ],
    "as":"event"
  }},
  {"#unwind" : "#event"},
  {"#group":{
    "_id":{
      "ai":"#ai",
      "av":"#av",
      "di":"#di",
			"dov":"#dov",
			"dn":"#dn",
			"ci":"#event.ci",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
    "csco":{"#sum":1}
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
		"di": "#_id.di",
		"dov": "#_id.dov",
		"dn": "#_id.dn",
		"ci": "#_id.ci",
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
		"csco": "#csco"
  }]}},
  {"#merge": {
    "into": "${IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "di", "dov", "dn", "ci"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "di" : "#di",
        "dov" : "#dov",
				"dn" : "#dn",
				"ci" : "#ci",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "csco" : {"#sum" : ["#csco" , "##new.csco"]}
      }}
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
			"di" : "#di",
			"dov" : "#dov",
			"dn" : "#dn",
			"ci" : "#ci"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
		"di": "#_id.di",
		"dov": "#_id.dov",
		"dn": "#_id.dn",
		"ci": "#_id.ci"
  }]}},
	{"#lookup":{
    "from": "${IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "di" : "#di", "dov" : "#dov", "dn" : "#dn", "ci" : "#ci"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
						{ "#eq": [ "#di",  "##di" ] },
						{ "#eq": [ "#dov",  "##dov" ] },
						{ "#eq": [ "#dn",  "##dn" ] },
						{ "#eq": [ "#ci",  "##ci" ] }
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
			"di":"#data.di",
			"dov":"#data.dov",
			"dn":"#data.dn",
			"ci":"#data.ci",
			"stz": "#data.stzd"
    },
    "csco":{"#sum":"#data.csco"}
  }},
	{"#group":{
    "_id":{
      "ai":"#_id.ai",
			"av":"#_id.av",
			"dov":"#_id.dov",
			"dn":"#_id.dn",
			"ci":"#_id.ci",
			"stz":"#_id.stz"
    },
    "csco":{"#sum":"#csco"},
    "cdco":{"#sum":1}
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stz": "#_id.stz",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "dov": "#_id.dov",
    "dn": "#_id.dn",
		"ci": "#_id.ci",
    "csco": "#csco",
		"cdco": "#cdco"
  }]}},
  {"#merge": {
    "into": "${IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "dov", "dn", "ci"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}
