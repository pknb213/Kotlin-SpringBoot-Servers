package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorDeviceByApp
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime


/**
 * indicator_device_by_app 컬렉션
 * 디바이스 수 (신규, 재방문)
 * adco, ndco, rdco
 */
object DeviceByAppBatch {

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
  {"#group":{
    "_id":{
      "ai":"#ai",
      "av":"#av",
      "di":"#di",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
		"adco": {"#sum":1},
		"rdco": {
      "#sum" : {
	     "#cond": [
	      {"#gt": ["#se", 1]},
	        1,
	        0
	      ]
      }},
    "ndco": {
      "#sum" : {
	     "#cond": [
	        {"#lte": ["#se", 1]},
	        1,
	        0
	     ]
		}}
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
		"di": "#_id.di",
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
		"adco":"#adco",
		"ndco":"#ndco",
		"rdco":"#rdco"
  }]}},
  {"#merge": {
    "into": "${IndicatorDeviceByApp.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "di"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
				"di" : "#di",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "adco" : {"#sum" : ["#adco" , "##new.adco"]},
				"ndco" : {"#sum" : ["#ndco" , "##new.ndco"]},
				"rdco" : {"#sum" : ["#rdco" , "##new.rdco"]}}
      }
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
      "di" : "#di"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "di" : "#_id.di"
  }]}},	
	{"#lookup":{
    "from": "${IndicatorDeviceByApp.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "di" : "#di"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
						{ "#eq": [ "#di",  "##di" ] }
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
			"stz": "#data.stzd"
    },
		"adco":{"#sum":"#data.adco"},
    "ndco":{"#sum":"#data.ndco"},
    "rdco":{"#sum":"#data.rdco"}
  }},
	
  {"#replaceWith":{ "#mergeObjects": [ {
    "stz": "#_id.stz",
    "ai": "#_id.ai",
    "av": "#_id.av",
		"adco":"#adco",
    "ndco":"#ndco",
    "rdco":"#rdco"
  }]}},
  {"#merge": {
    "into": "${IndicatorDeviceByApp.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorDeviceByApp.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}