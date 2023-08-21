package io.userhabit.batch

import io.userhabit.batch.indicators.CohortPreview
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import io.userhabit.polaris.EventType as ET

object PreviewByCohort {
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
  {"#lookup" : {
		"from" : "event",
		"let" : {"si" : "#_id"},
		"pipeline" : [
			{"#match" : {"#expr" : {"#and" : [
				{"#eq" : ["#_id.si", "#{'#'}#si"]},
				{"#eq" : ["#t", ${ET.VIEW_START}]}
			]}}},
			{"#group" : {
				"_id" : {
					"si" : "#_id.si"
				},
				"data" : {"#push" : {
					"vhi" : "#vhi",
					"ts" : "#_id.ts"
				}}
			}},
			{"#addFields" : {
				"first" : { "#arrayElemAt": [ "#data.vhi", 0 ] },
				"last" : { "#arrayElemAt": [ "#data.vhi", -1 ] },
				"dt" : { "#arrayElemAt": [ "#data.ts", -1 ] },
				"view_count" : {"#size" : "#data"}
			}},
			{"#unwind" : "#data"},
			{"#group" : {
				"_id" : {
					"si" : "#si",
					"vhi" : "#data.vhi",
					"first" : "#first",
					"last" : "#last",
					"dt" : "#dt",
					"total_view_count" : "#view_count"
				},
				"each_view_count" : {"#sum" : 1}
			}},
			{"#group" : {
				"_id" : {
					"si" : "#_id.si",
					"first" : "#_id.first",
					"last" : "#_id.last",
					"dt" : "#_id.dt",
					"total_view_count" : "#_id.total_view_count"
				},
				"unique_view_count" : {"#sum" : 1},
				"data" : {"#push" : {
					"vhi" : "#_id.vhi",
					"each_view_count" : "#each_view_count"
				}}
			}}
		],
		"as" : "event"
  }},
  {"#lookup": {
	"from": "app",
	"localField": "ai",
	"foreignField": "_id",
	"as": "app"
  }},
	{"#replaceWith" : {"#mergeObjects" : {
		"st" : "#st",
		"stz": { "#toDate" : { "#dateToString" : {
			"date": "#st",
			"timezone" : {"#arrayElemAt":["#app.time_zone", 0]}
		}}},
		"si" : "#_id",
		"sn" : "#sn",
		"av" : "#av",
		"ai" : "#ai",
		"di" : "#di",
		"dl" : "#dl",
		"dz" : "#dz",
		"first_view" : {"#arrayElemAt":["#event._id.first", 0]},
		"last_view" : {"#arrayElemAt":["#event._id.last", 0]},
		"dt" : {"#arrayElemAt":["#event._id.dt", 0]},
		"total_view_count" : {"#arrayElemAt":["#event._id.total_view_count", 0]},
		"unique_view_count" : {"#arrayElemAt":["#event.unique_view_count", 0]},
		"view_count" : {"#arrayElemAt":["#event.data", 0]}
	}}},
  {"#merge": {
    "into": "${CohortPreview.COLLECTION_NAME_PT10M}",
    "on": ["stz", "ai", "av", "si"],
    "whenMatched": "merge",
    "whenNotMatched": "insert"}}
]
        """
		return BatchUtil.run(object : Any() {}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}
}