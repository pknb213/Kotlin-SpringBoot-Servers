package io.userhabit.batch

import io.userhabit.batch.indicators.CrashList
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import io.userhabit.polaris.EventType as ET

object CrashListBatch {

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
  {"#unwind":"#event"},
  {"#group":{
    "_id":{
      "ai": "#ai",
      "av": "#av",
			"si": "#_id",
			"dn": "#dn",
			"dov": "#dov",
      "ci": "#event.ci",
			"cm": "#event.cm",
			"ct": "#event.ct",
			"cs": "#event.cs",
			"cb": "#event.cb",
			"cc": "#event.cc",
			"cfd": "#event.cfd",
			"ctd": "#event.ctd", 
			"cfm": "#event.cfm",
			"ctm": "#event.ctm",
			"cn": "#event.cn",
			"cp": "#event.cp",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    }
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
		"si": "#_id.si",
		"dn": "#_id.dn",
		"dov": "#_id.dov",
    "ci": "#_id.ci",
    "cm": "#_id.cm",
    "ct": "#_id.ct",
    "cs": "#_id.cs",
		"cb":	"#_id.cb",
		"cc":	"#_id.cc",
		"cfd": "#_id.cfd",
		"ctd": "#_id.ctd", 
		"cfm": "#_id.cfm",
		"ctm": "#_id.ctm",
		"cn": "#_id.cn",
		"cp": "#_id.cp",
    "stz": { "#toDate" : { "#dateToString" : {
      "date": "#_id.st",
      "timezone" : {"#arrayElemAt":["#app.time_zone", 0]}
    }}},
    "bft": {"#toDate": "${fromDt.format(BatchUtil.toDateFormater)}"}
  }]}},
  {"#merge": {
    "into": "${CrashList.COLLECTION_NAME_PT10M}",
    "on": ["stz", "ai", "av", "si", "ci"],
    "whenMatched": "merge",
    "whenNotMatched": "insert"}}
]
        """
		return BatchUtil.run(object : Any() {}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}
}
