package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorDeviceByOsDevicename
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime

/**
 * indicator_device_by_os_devicename 컬렉션
 * 디바이스 수  (OS, 디바이스 별)
 * dco
 */
object DeviceByOsDevicenameBatch {

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
        "dov":"#dov",
        "dn":"#dn",
        "st": {"#toDate":{"#concat":[
            {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
            "0:00"
        ]}}
    },
    "dco": {"#sum":1}
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
    "dco": "#dco"
  }]}},
  {"#merge": {
    "into": "${IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "di", "dov", "dn"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "di" : "#di",
        "dov" : "#dov",
        "dn" : "#dn",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "dco" : {"#sum" : ["#dco" , "##new.dco"]}}
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
        "di" : "#di",
        "dov" : "#dov",
        "dn" : "#dn"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "di": "#_id.di",
    "dov": "#_id.dov",
    "dn" : "#_id.dn"
  }]}},
  {"#lookup":{
    "from": "${IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "di" : "#di", "dov" : "#dov", "dn" : "#dn"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd"]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
            { "#eq": [ "#di",  "##di" ] },
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
        "di":"#data.di",
        "dov":"#data.dov",
        "dn":"#data.dn",
        "stz":"#data.stzd"
    },
    "dco":{"#sum":"#data.dco"}
  }},
  
  {"#replaceWith":{ "#mergeObjects": [ {
    "stz": "#_id.stz",
    "ai": "#_id.ai",
    "av": "#_id.av",
    "di": "#_id.di",
    "dov": "#_id.dov",
    "dn": "#_id.dn",
    "dco": "#dco"
  }]}},
  {"#merge": {
    "into": "${IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "di", "dov", "dn"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
	return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT10M, fromDt, toDt)
 }
}