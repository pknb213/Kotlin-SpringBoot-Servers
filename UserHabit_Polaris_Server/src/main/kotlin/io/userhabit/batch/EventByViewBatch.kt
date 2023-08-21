package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorEventByView
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import io.userhabit.polaris.EventType as ET

/**
 * 이벤트 지표
 * 본 indicator_event_by_view 컬렉션의 키 및 인덱스
 * stz, ai, av, vhi, t
 * 뷰의 다음 이벤트(4353, 4354, 4096, 4097)를 제외한 모든 이벤트 수 합 = eco
 * 변경된 다음 이벤트 값 (4101, 4100, 8801, 8800)
 * @author cjh
 */
object EventByViewBatch {
	/**
	 * 뷰의 이벤트 수 합 10분
	 * 서버에 세션 종료 이벤트가 들어온 시간(i)을 기준으로 함 => 서버에 세션 이벤트가 들어왔다는 것 = 액션도 마무리 되었다
	 * 추가로 i 값이 없고, 세션 시작 시간(st)가 1일 경과한 세션은 종료된 세션으로 판단하여 배치 범위에 포함
	 * @author cjh
	 */
	fun tenMinutes(fromDt: ZonedDateTime, toDt: ZonedDateTime) : Mono<String> {
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
            {"#not" : { "#in": [ "#t", [
							${ET.VIEW_START},
							${ET.VIEW_END},
							${ET.APP_START},
							${ET.APP_END} ]]
						}}
          ]}
        }
      }
    ],
    "as":"event"
  }},
  {"#unwind" : "#event"},
  {"#group" : {
    "_id" : {
      "ai":"#ai",
      "av":"#av",
      "vhi":"#event.ofvhi",
      "t":"#event.t",
      "st": {"#toDate":{"#concat":[
        {"#substr": [{"#dateToString": { "format": "%Y-%m-%d %H:%M", "date": "#st"}}, 0, 15]},
        "0:00"
      ]}}
    },
    "eco" : {"#sum" : 1}
  }},
  {"#lookup": {
    "from": "app",
    "localField": "_id.ai",
    "foreignField": "_id",
    "as": "app"
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "vhi": "#_id.vhi",
    "av": "#_id.av",
    "st": "#_id.st",
    "t": "#_id.t",
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
    "eco": "#eco"
  }]}},
  {"#merge": {
    "into": "${IndicatorEventByView.COLLECTION_NAME_PT10M}",
    "on": ["st", "ai", "av", "vhi", "t"],
    "whenMatched": [
      {"#addFields" : {
        "st" : "#st",
        "ai" : "#ai",
        "av" : "#av",
        "vhi" : "#vhi",
				"t" : "#t",
        "stz" : "##new.stz",
        "stzd" : "##new.stzd",
        "bft" : "##new.bft",
        "eco" : {"#sum" : ["#eco" , "##new.eco"]}}
      }
    ],
    "whenNotMatched": "insert"}}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
	}

	/**
	 * 뷰의 이벤트 수 합 1일
	 * 10분 배치 완료 시간(bft)을 기준으로 함.
	 * 그룹 이전에 자기 자신의 collection을 lookup하여 stz(앱 타임존을 적용한 st) 의 날짜가 일치하는 것을 전부 불러와서 다시 계산함
	 * @author cjh
	 */
	fun oneDay(fromDt: ZonedDateTime, toDt: ZonedDateTime) : Mono<String> {
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
      "t" : "#t"
    }
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "stzd": "#_id.stzd",
    "ai": "#_id.ai",
    "av": "#_id.av",
		"vhi": "#_id.vhi",
    "t" : "#_id.t"
  }]}},
	{"#lookup":{
    "from": "${IndicatorEventByView.COLLECTION_NAME_PT10M}",
    "let": { "stzd": "#stzd", "ai" : "#ai", "av" : "#av", "vhi" : "#vhi", "t" : "#t"},
    "pipeline": [
      { "#match":
        { "#expr":
          { "#and": [
            { "#eq": [ "#stzd", "##stzd" ]},
            { "#eq": [ "#ai",  "##ai" ] },
            { "#eq": [ "#av",  "##av" ] },
						{ "#eq": [ "#vhi",  "##vhi" ] },
						{ "#eq": [ "#t",  "##t" ] }
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
      "t":"#data.t",
			"stz": "#data.stzd"
    },
    "eco":{"#sum":"#data.eco"}
  }},
  {"#replaceWith":{ "#mergeObjects": [ {
    "ai": "#_id.ai",
    "vhi": "#_id.vhi",
    "av": "#_id.av",
    "t": "#_id.t",
    "stz": "#_id.stz",
    "eco": "#eco"
  }]}},
  {"#merge": {
    "into": "${IndicatorEventByView.COLLECTION_NAME_PT24H}",
    "on": ["stz", "ai", "av", "vhi", "t"],
    "whenMatched": "merge",
    "whenNotMatched": "insert" }}
]
    """
		return BatchUtil.run(object : Any(){}.javaClass, query, IndicatorEventByView.COLLECTION_NAME_PT10M, fromDt, toDt)
	}
}