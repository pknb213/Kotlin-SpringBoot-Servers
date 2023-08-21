package io.userhabit.batch

import io.userhabit.batch.indicators.IndicatorHeatmapByView
import io.userhabit.polaris.service.SessionService
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import io.userhabit.polaris.EventType as ET
import org.bson.Document as D

/**
 * 유저해빗에서 분석하는 히트맵 디바이스의 비율
 * 16:7 ~ 16:8.5 사이의 디바이스만 분석한다
 * @author cjh
 */
// Todo : 83 Line Delete -> { "#eq": [ "#vhi",  "##vhi" ] }, is Wrong?
object HeatmapByViewBatch {
    /**
     *
     * db.session.aggregate([
     *   {
     *     "$match": {
     *       "$or": [
     *         {
     *           "i": {
     *             "$gte": ISODate("2022-03-03"),
     *             "$lt": ISODate("2022-04-04"),
     *           }
     *         },
     *         {
     *           "$and": [
     *             {
     *               "i": {
     *                 "$exists": false
     *               }
     *             },
     *             {
     *               "st": {
     *                 "$gte": ISODate("2022-04-06T07:00:00Z"),
     *                 "$lt": ISODate("2022-04-06T07:10:00Z"),
     *               }
     *             }
     *           ]
     *         }
     *       ]
     *     }
     *   },
     *   {
     *     "$lookup": {
     *       "from": "event",
     *       "let": {
     *         "si": "$_id"
     *       },
     *       "pipeline": [
     *         {
     *           "$match": {
     *             "$expr": {
     *               "$and": [
     *                 {
     *                   "$eq": ["$_id.si", "$$si"]
     *                 },
     *                 {
     *                   "$eq": ["$t", 4101]
     *                 },
     *                 {
     *                   "$ne": ["$vw", 0]
     *                 },
     *                 {
     *                   "$ne": ["$vh", 0]
     *                 }
     *               ]
     *             }
     *           }
     *         },
     *         {
     *           "$group": {
     *             "_id": {
     *               "vhi": "$vhi",
     *               "vo": {
     *                 "$cond": [
     *                   { "$gte": ["$vh", "$vw"] }, 1, 0
     *                 ]
     *               },
     *               "uts": "$uts"
     *             }
     *           }
     *         }
     *       ],
     *       "as": "view"
     *     }
     *   },
     *   {
     *     "$unwind": "$view"
     *   },
     *   {
     *     "$lookup": {
     *       "from": "event",
     *       "localField": "_id",
     *       "foreignField": "_id.si",
     *       "as": "event"
     *     }
     *   },
     *   {
     *     "$project": {
     *       "_id": 0,
     *       "si": "$_id",
     *       "ai": "$ai",
     *       "av": "$av",
     *       "dw": "$dw",
     *       "dh": "$dh",
     *       "st": "$st",
     *       "vhi": "$view._id.vhi",
     *       "vo": "$view._id.vo",
     *       "uts": "$view._id.uts",
     *       "eventcand": {
     *         "$filter": {
     *           "input": "$event",
     *           "as": "event",
     *           "cond": {
     *             "$and": [
     *               {
     *                 "$in": [
     *                   "$$event.t", [1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108]
     *                 ]
     *               },
     *             ]
     *           }
     *         }
     *       },
     *       "event": {
     *         "$filter": {
     *           "input": "$event",
     *           "as": "event",
     *           "cond": {
     *             "$and": [
     *               {
     *                 "$in": [
     *                   "$$event.t", [1101, 1102, 1103, 1104, 1001, 1002, 1003, 1004]
     *                 ]
     *               },
     *               {
     *                 "$eq": [
     *                   "$$event.uts",
     *                   "$view._id.uts"
     *                 ]
     *               },
     *               {
     *                 "$ne": ["$$event.gx", -1]
     *               },
     *               {
     *                 "$ne": ["$$event.gy", -1]
     *               },
     *               {
     *                 "$lte": [
     *                   "$$event.gx",
     *                   {
     *                     "$cond": [
     *                       {
     *                         "$eq": ["$view._id.vo", 1]
     *                       },
     *                       "$dw",
     *                       "$dh"
     *                     ]
     *                   }
     *                 ]
     *               },
     *               {
     *                 "$lte": [
     *                   "$$event.gy",
     *                   {
     *                     "$cond": [
     *                       {
     *                         "$eq": ["$view._id.vo", 1]
     *                       },
     *                       "$dh",
     *                       "$dw"
     *                     ]
     *                   }
     *                 ]
     *               }
     *             ]
     *           }
     *         }
     *       }
     *     }
     *   },
     *   {
     *     $project: {
     *       ai: 1,
     *       av: 1,
     *       dw: 1,
     *       dh: 1,
     *       st: 1,
     *       vhi: 1,
     *       vo: 1,
     *       uts: 1,
     *       event: 1,
     *       fevent: { $min: "$eventcand.ts" },
     *       levent: { $max: "$eventcand.ts" },
     *     }
     *   },
     *   {
     *     "$unwind": "$event"
     *   },
     *   {
     *     "$group": {
     *       "_id": {
     *         "ai": "$ai",
     *         "av": "$av",
     *         "vhi": "$vhi",
     *         "fevent": {$eq : ["$fevent", "$event.ts"]},
     *         "levent": {$eq : ["$levent", "$event.ts"]},
     *         "hx": {
     *           "$round": [
     *             {
     *               "$cond": [
     *                 {
     *                   "$eq": ["$vo", 1]
     *                 },
     *                 {
     *                   "$divide": ["$event.gx", "$dw"]
     *                 },
     *                 {
     *                   "$divide": ["$event.gx", "$dh"]
     *                 }
     *               ]
     *             },
     *             2
     *           ]
     *         },
     *         "hy": {
     *           "$round": [
     *             {
     *               "$cond": [
     *                 {
     *                   "$eq": ["$vo", 1]
     *                 },
     *                 {
     *                   "$divide": ["$event.gy", "$dh"]
     *                 },
     *                 {
     *                   "$divide": ["$event.gy", "$dw"]
     *                 }
     *               ]
     *             },
     *             2
     *           ]
     *         },
     *         "hex": {
     *           "$round": [
     *             {
     *               "$cond": [
     *                 {
     *                   "$eq": ["$vo", 1]
     *                 },
     *                 {
     *                   "$divide": ["$event.gex", "$dw"]
     *                 },
     *                 {
     *                   "$divide": ["$event.gex", "$dh"]
     *                 }
     *               ]
     *             },
     *             2
     *           ]
     *         },
     *         "hey": {
     *           "$round": [
     *             {
     *               "$cond": [
     *                 {
     *                   "$eq": ["$vo", 1]
     *                 },
     *                 {
     *                   "$divide": ["$event.gey", "$dh"]
     *                 },
     *                 {
     *                   "$divide": ["$event.gey", "$dw"]
     *                 }
     *               ]
     *             },
     *             2
     *           ]
     *         },
     *         "t": "$event.t",
     *         "vo": "$vo",
     *         "st": {
     *           "$toDate": {
     *             "$concat": [
     *               {
     *                 "$substr": [
     *                   { "$dateToString": { "format": "%Y-%m-%d %H:%M", "date": "$st" } },
     *                   0, 15
     *                 ]
     *               },
     *               "0:00"
     *             ]
     *           }
     *         }
     *       },
     *       "count": {
     *         "$sum": 1
     *       }
     *     }
     *   },
     *   {
     *     "$lookup": {
     *       "from": "app",
     *       "localField": "_id.ai",
     *       "foreignField": "_id",
     *       "as": "app"
     *     }
     *   },
     *   {
     *     "$replaceWith": {
     *       "$mergeObjects": [
     *         {
     *           "ai": "$_id.ai",
     *           "av": "$_id.av",
     *           "st": "$_id.st",
     *           "stz": {
     *             "$toDate": {
     *               "$dateToString": {
     *                 "date": "$_id.st",
     *                 "timezone": {
     *                   "$arrayElemAt": ["$app.time_zone", 0]
     *                 }
     *               }
     *             }
     *           },
     *           "vhi": "$_id.vhi",
     *           "hx": "$_id.hx",
     *           "hy": "$_id.hy",
     *           "hex": {
     *             "$ifNull": ["$_id.hex", 0]
     *           },
     *           "hey": {
     *             "$ifNull": ["$_id.hey", 0]
     *           },
     *           "t": "$_id.t",
     *           "vo": "$_id.vo",
     *           "bft": {
     *             "$toDate": "2022-04-07T07:00:00Z"
     *           },
     *           "fevent": "$_id.fevent",
     *           "levent": "$_id.levent",
     *           "count": "$count"
     *         }
     *       ]
     *     }
     *   },
     *   {
     *     "$merge": {
     *       "into": "indicator_heatmap_by_view_pt10m",
     *       "on": [
     *         "stz",
     *         "st",
     *         "ai",
     *         "av",
     *         "vhi",
     *         "t",
     *         "vo",
     *         "hx",
     *         "hy",
     *         "hex",
     *         "hey",
     *         "fevent",
     *         "levent",
     *       ],
     *       "whenMatched": [
     *         {
     *           "$addFields": {
     *             "st": "$st",
     *             "ai": "$ai",
     *             "av": "$av",
     *             "vhi": "$vhi",
     *             "t": "$t",
     *             "vo": "$vo",
     *             "hx": "$hx",
     *             "hy": "$hy",
     *             "hex": "$hex",
     *             "hey": "$hey",
     *             "stz": "$$new.stz",
     *             "bft": "$$new.bft",
     *             "count": {
     *               "$sum": [
     *                 "$count",
     *                 "$$new.count"
     *               ]
     *             }
     *           }
     *         }
     *       ],
     *       "whenNotMatched": "insert"
     *     }
     *   }
     * ])
     */

    fun tenMinutes(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
        val query = listOf(
            this._getMatchQuery(fromDt, toDt),
            this._getLookupViewStartQuery(),
            D(mapOf("\$unwind" to "\$view")),
            this._getLookupEventQuery(),
            this._getProjectFilterEventQuery(),
            this._getProjectFirstLastActionEventQuery(),
            D(mapOf("\$unwind" to "\$event")),
            this._getGroupQuery(),
            this._getLookupAppQuery(),
            this._getReplaceWith(fromDt),
            this._getMergeQuery()
        )

        val queryStr = this._getQueryString(query)
        return BatchUtil.run(object : Any() {}.javaClass, queryStr, SessionService.COLLECTION_NAME, fromDt, toDt)
    }

    /**
     * {
     *   "$match": {
     *     "$or": [
     *       {
     *         "i": {
     *           "$gte": ISODate("2022-03-03"),
     *           "$lt": ISODate("2022-04-04"),
     *         }
     *       },
     *       {
     *         "$and": [
     *           {
     *             "i": {
     *               "$exists": false
     *             }
     *           },
     *           {
     *             "st": {
     *               "$gte": ISODate("2022-04-06T07:00:00Z"),
     *               "$lt": ISODate("2022-04-06T07:10:00Z"),
     *             }
     *           }
     *         ]
     *       }
     *     ]
     *   }
     * },
     */
    private fun _getMatchQuery(fromDt: ZonedDateTime, toDt: ZonedDateTime): D {
        val res = D("\$match", D("\$or", listOf(
            D("i", D(
                "\$gte", Date.from(fromDt.toInstant()))
                .append("\$lt", Date.from(toDt.toInstant()))
            ),
            D("\$and", listOf(
                D("i", D("\$exists", false)),
                D("st", D(
                    "\$gte", Date.from(fromDt.minusDays(1).toInstant()))
                    .append("\$lt", Date.from(toDt.minusDays(1).toInstant()))
                ),
            ))
        )))

        return res
    }

    /**
     * {
     *   "$lookup": {
     *     "from": "event",
     *     "let": {
     *       "si": "$_id"
     *     },
     *     "pipeline": [
     *       {
     *         "$match": {
     *           "$expr": {
     *             "$and": [
     *               { "$eq": ["$_id.si", "$$si"] },
     *               { "$eq": ["$t", 4101] },
     *               { "$ne": ["$vw", 0] },
     *               { "$ne": ["$vh", 0] }
     *             ]
     *           }
     *         }
     *       },
     *       {
     *         "$group": {
     *           "_id": {
     *             "vhi": "$vhi",
     *             "vo": {
     *               "$cond": [ { "$gte": ["$vh", "$vw"] }, 1, 0 ]
     *             },
     *             "uts": "$uts"
     *           }
     *         }
     *       }
     *     ],
     *     "as": "view"
     *   }
     * },
     */
    private fun _getLookupViewStartQuery(): D {
        val res = D(
            "\$lookup", D(
            "from", "event")
            .append("let", D("si", "\$_id"))
            .append("pipeline", listOf(
                D("\$match",
                    D("\$expr", D("\$and", listOf(
                        D("\$eq", listOf("\$_id.si", "\$\$si")),
                        D("\$eq", listOf("\$t", ET.VIEW_START)),
                        D("\$ne", listOf("\$vw", 0)),
                        D("\$ne", listOf("\$vh", 0))
                    )))),
                D("\$group",
                    D("_id", D(
                        "vhi", "\$vhi")
                        // Todo: 추후에 해상도에 맞도록 조절해야 함.
                        .append("vo", D("\$cond", listOf(D("\$gte", listOf("\$vh", "\$vw")), 1, 0)))
                        .append("uts", "\$uts")
                    ))
            )).append("as", "view")
        )
        return res
    }

    /**
     * {
     *   "$lookup": {
     *     "from": "event",
     *     "localField": "_id",
     *     "foreignField": "_id.si",
     *     "as": "event"
     *   }
     * },
     */
    private fun _getLookupEventQuery(): D {
        return D("\$lookup", D("from", "event")
            .append("localField", "_id")
            .append("foreignField", "_id.si")
            .append("as", "event")
        )
    }

    /**
     * {
     *   "$project": {
     *     "_id": 0,
     *     "si": "$_id",
     *     "ai": "$ai",
     *     "av": "$av",
     *     "dw": "$dw",
     *     "dh": "$dh",
     *     "st": "$st",
     *     "vhi": "$view._id.vhi",
     *     "vo": "$view._id.vo",
     *     "uts": "$view._id.uts",
     *     "eventcand": {
     *       "$filter": {
     *         "input": "$event",
     *         "as": "event",
     *         "cond": {
     *           "$and": [
     *             {
     *               "$in": [
     *                 "$$event.t", [1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108]
     *               ]
     *             },
     *           ]
     *         }
     *       }
     *     },
     *     "event": {
     *       "$filter": {
     *         "input": "$event",
     *         "as": "event",
     *         "cond": {
     *           "$and": [
     *             {
     *               "$in": [
     *                 "$$event.t", [
     *                   1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108,
     *                   1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008,]
     *               ]
     *             },
     *             {
     *               "$eq": [
     *                 "$$event.uts",
     *                 "$view._id.uts"
     *               ]
     *             },
     *             {
     *               "$ne": ["$$event.gx", -1]
     *             },
     *             {
     *               "$ne": ["$$event.gy", -1]
     *             },
     *             {
     *               "$lte": [
     *                 "$$event.gx",
     *                 {
     *                   "$cond": [
     *                     { "$eq": ["$view._id.vo", 1] },
     *                     "$dw",
     *                     "$dh"
     *                   ]
     *                 }
     *               ]
     *             },
     *             {
     *               "$lte": [
     *                 "$$event.gy",
     *                 {
     *                   "$cond": [
     *                     { "$eq": ["$view._id.vo", 1] },
     *                     "$dh",
     *                     "$dw"
     *                   ]
     *                 }
     *               ]
     *             }
     *           ]
     *         }
     *       }
     *     }
     *   }
     * },
     */
    private fun _getProjectFilterEventQuery(): D {
        val ret = D("\$project", D("_id", 0)
            .append("si", "\$_id")
            .append("ai", "\$ai")
            .append("av", "\$av")
            .append("dw", "\$dw")
            .append("dh", "\$dh")
            .append("st", "\$st")
            .append("vhi", "\$view._id.vhi")
            .append("vo", "\$view._id.vo")
            .append("uts", "\$view._id.uts")
            // 'eventcand' : to get 'first action' and 'last action'
            .append("eventcand", D("\$filter", D("input", "\$event")
                .append("as", "event")
                .append("cond",
                    D("\$and", listOf(
                        D("\$in", listOf("\$\$event.t", listOf(
                            ET.REACT_TAP,
                            ET.REACT_DOUBLE_TAP,
                            ET.REACT_LONG_TAP,
                            ET.REACT_SWIPE,
                            ET.REACT_PAN_START,
                            ET.REACT_PAN_MOVE,
                            ET.REACT_PAN_END,
                            ET.REACT_SECRET_TAP,
                        ))),
                    ))
                )))
            .append("event", D("\$filter", D("input", "\$event")
                .append("as", "event")
                .append("cond",
                    D("\$and", listOf(
                        D("\$in", listOf("\$\$event.t", listOf(
                            ET.REACT_TAP,
                            ET.REACT_DOUBLE_TAP,
                            ET.REACT_LONG_TAP,
                            ET.REACT_SWIPE,
                            ET.REACT_PAN_START,
                            ET.REACT_PAN_MOVE,
                            ET.REACT_PAN_END,
                            ET.REACT_SECRET_TAP,
                            ET.NOACT_TAP,
                            ET.NOACT_DOUBLE_TAP,
                            ET.NOACT_LONG_TAP,
                            ET.NOACT_SWIPE,
                            ET.NOACT_PAN_START,
                            ET.NOACT_PAN_MOVE,
                            ET.NOACT_PAN_END,
                            ET.NOACT_SECRET_TAP,
                        ))),
                        D("\$eq", listOf("\$\$event.uts", "\$view._id.uts")),
                        D("\$ne", listOf("\$\$event.gx", -1)),
                        D("\$ne", listOf("\$\$event.gy", -1)),
                        D("\$lte", listOf("\$\$event.gx", D("\$cond", listOf(D("\$eq", listOf("\$view._id.vo", 1)), "\$dw", "\$dh")))),
                        D("\$lte", listOf("\$\$event.gy", D("\$cond", listOf(D("\$eq", listOf("\$view._id.vo", 1)), "\$dh", "\$dw"))))
                    ))
                )))
        )
        return ret
    }


    /**
     *
     * {
     *   $project: {
     *     ai: 1,
     *     av: 1,
     *     dw: 1,
     *     dh: 1,
     *     st: 1,
     *     vhi: 1,
     *     vo: 1,
     *     uts: 1,
     *     event: 1,
     *     fevent: { $min: "$eventcand.ts" },
     *     levent: { $max: "$eventcand.ts" },
     *   }
     * },
     */
    private fun _getProjectFirstLastActionEventQuery(): D {
        return D("\$project",
            D(
                "ai", 1
            )
                .append("av", 1)
                .append("dw", 1)
                .append("dh", 1)
                .append("st", 1)
                .append("vhi", 1)
                .append("vo", 1)
                .append("uts", 1)
                .append("event", 1)
                .append("fevent", D("\$min", "\$eventcand.ts"))
                .append("levent", D("\$max", "\$eventcand.ts"))
        )
    }

    /**
     * {
     *   "$group": {
     *     "_id": {
     *       "ai": "$ai",
     *       "av": "$av",
     *       "vhi": "$vhi",
     *       "hx": {
     *         "$round": [
     *           {
     *             "$cond": [
     *               { "$eq": ["$vo", 1] },
     *               { "$divide": ["$event.gx", "$dw"] },
     *               { "$divide": ["$event.gx", "$dh"] }
     *             ]
     *           },
     *           2
     *         ]
     *       },
     *       "hy": {
     *         "$round": [
     *           {
     *             "$cond": [
     *               { "$eq": ["$vo", 1] },
     *               { "$divide": ["$event.gy", "$dh"] },
     *               { "$divide": ["$event.gy", "$dw"] }
     *             ]
     *           },
     *           2
     *         ]
     *       },
     *       "hex": {
     *         "$round": [
     *           {
     *             "$cond": [
     *               { "$eq": ["$vo", 1] },
     *               { "$divide": ["$event.gex", "$dw"] },
     *               { "$divide": ["$event.gex", "$dh"] }
     *             ]
     *           },
     *           2
     *         ]
     *       },
     *       "hey": {
     *         "$round": [
     *           {
     *             "$cond": [
     *               { "$eq": ["$vo", 1] },
     *               { "$divide": ["$event.gey", "$dh"] },
     *               { "$divide": ["$event.gey", "$dw"] }
     *             ]
     *           },
     *           2
     *         ]
     *       },
     *       "t": "$event.t",
     *       "vo": "$vo",
     *       "st": {
     *         "$toDate": {
     *           "$concat": [
     *             {
     *               "$substr": [
     *                 { "$dateToString": { "format": "%Y-%m-%d %H:%M", "date": "$st" } },
     *                 0, 15
     *               ]
     *             },
     *             "0:00"
     *           ]
     *         }
     *       }
     *     },
     *     "count": {
     *       "$sum": 1
     *     }
     *   }
     * },
     */
    private fun _getGroupQuery(): D {
        val ret = D("\$group", D(
            "_id", D(
            "ai", "\$ai")
            .append("av", "\$av")
            .append("vhi", "\$vhi")
            .append("fevent", D("\$eq", listOf("\$fevent", "\$event.ts")))
            .append("levent", D("\$eq", listOf("\$levent", "\$event.ts")))
            .append("hx",
                D("\$round", listOf(
                    D("\$cond", listOf(
                        D("\$eq", listOf("\$vo", 1)),
                        D("\$divide", listOf("\$event.gx", "\$dw")),
                        D("\$divide", listOf("\$event.gx", "\$dh"))
                    )),
                    2
                ))
            ).append("hy",
                D("\$round", listOf(
                    D("\$cond", listOf(
                        D("\$eq", listOf("\$vo", 1)),
                        D("\$divide", listOf("\$event.gy", "\$dh")),
                        D("\$divide", listOf("\$event.gy", "\$dw"))
                    )),
                    2
                ))
            ).append("hex",
                D("\$round", listOf(
                    D("\$cond", listOf(
                        D("\$eq", listOf("\$vo", 1)),
                        D("\$divide", listOf("\$event.gex", "\$dw")),
                        D("\$divide", listOf("\$event.gex", "\$dh"))
                    )),
                    2
                )))
            .append("hey",
                D("\$round", listOf(
                    D("\$cond", listOf(
                        D("\$eq", listOf("\$vo", 1)),
                        D("\$divide", listOf("\$event.gey", "\$dh")),
                        D("\$divide", listOf("\$event.gey", "\$dw"))
                    )),
                    2
                )))
            .append("t", "\$event.t")
            .append("vo", "\$vo")
            .append("st",
                D("\$toDate",
                    D("\$concat", listOf(
                        D("\$substr", listOf(
                            D("\$dateToString", D("format", "%Y-%m-%d %H:%M").append("date", "\$st")),
                            0, 15
                        )),
                        "0:00"
                    ))))
        ).append(
            "count", D("\$sum", 1)
        ))
        return ret
    }


    /**
     * {
     *   "$lookup": {
     *     "from": "event",
     *     "localField": "_id.ai",
     *     "foreignField": "_id",
     *     "as": "app"
     *   }
     * },
     */
    private fun _getLookupAppQuery(): D {
        return D("\$lookup", D("from", "app")
            .append("localField", "_id.ai")
            .append("foreignField", "_id")
            .append("as", "app")
        )
    }

    /**
     * This stage do the followings:
     *
     * - formatting
     * - add 'bft' field
     * - add 'stz' field
     *
     * {
     *   "$replaceWith": {
     *     "$mergeObjects": [
     *       {
     *         "ai": "$_id.ai",
     *         "av": "$_id.av",
     *         "st": "$_id.st",
     *         "stz": {
     *           "$toDate": {
     *             "$dateToString": {
     *               "date": "$_id.st",
     *               "timezone": {
     *                 "$arrayElemAt": ["$app.time_zone", 0]
     *               }
     *             }
     *           }
     *         },
     *         "vhi": "$_id.vhi",
     *         "hx": "$_id.hx",
     *         "hy": "$_id.hy",
     *         "hex": {
     *           "$ifNull": ["$_id.hex", 0]
     *         },
     *         "hey": {
     *           "$ifNull": ["$_id.hey", 0]
     *         },
     *         "t": "$_id.t",
     *         "vo": "$_id.vo",
     *         "bft": {
     *           "$toDate": "2022-04-07T07:00:00Z"
     *         },
     *         "fevent": "$_id.fevent",
     *         "levent": "$_id.levent",
     *         "count": "$count"
     *       }
     *     ]
     *   }
     * },
     */
    private fun _getReplaceWith(fromDt: ZonedDateTime): D {
        return D("\$replaceWith",
            D("\$mergeObjects", listOf(
                D("ai", "\$_id.ai")
                    .append("av", "\$_id.av")
                    .append("st", "\$_id.st")
                    .append("stz",
                        D("\$toDate",
                            D("\$dateToString",
                                D("date", "\$_id.st")
                                    .append("timezone", D("\$arrayElemAt", listOf("\$app.time_zone", 0)))
                            )
                        )
                    ).append("vhi", "\$_id.vhi")
                    .append("hx", "\$_id.hx")
                    .append("hy", "\$_id.hy")
                    .append("hex", D("\$ifNull", listOf("\$_id.hex", 0)))
                    .append("hey", D("\$ifNull", listOf("\$_id.hey", 0)))
                    .append("t", "\$_id.t")
                    .append("vo", "\$_id.vo")
                    .append("bft", D("\$toDate", fromDt.format(BatchUtil.toDateFormater)))
                    .append("fevent", "\$_id.fevent")
                    .append("levent", "\$_id.levent")
                    .append("count", "\$count")
            ))
        )
    }

    /**
     * the reason of using `$sum`
     *
     * - https://github.com/userhabit/uh-issues/issues/851#issuecomment-1092465097
     *
     * {
     *   "$merge": {
     *     "into": "indicator_heatmap_by_view_pt10m",
     *     "on": [
     *       "stz",
     *       "st",
     *       "ai",
     *       "av",
     *       "vhi",
     *       "t",
     *       "vo",
     *       "hx",
     *       "hy",
     *       "hex",
     *       "hey",
     *       "fevent",
     *       "levent",
     *     ],
     *     "whenMatched": [
     *       {
     *         "$addFields": {
     *           "st": "$st",
     *           "ai": "$ai",
     *           "av": "$av",
     *           "vhi": "$vhi",
     *           "t": "$t",
     *           "vo": "$vo",
     *           "hx": "$hx",
     *           "hy": "$hy",
     *           "hex": "$hex",
     *           "hey": "$hey",
     *           "stz": "$$new.stz",
     *           "bft": "$$new.bft",
     *           "fevent",
     *           "levent",
     *           "count": {
     *             "$sum": [
     *               "$count",
     *               "$$new.count"
     *             ]
     *           }
     *         }
     *       }
     *     ],
     *     "whenNotMatched": "insert"
     *   }
     * }
     */
    private fun _getMergeQuery(): D {

        return D("\$merge",
            D("into", IndicatorHeatmapByView.COLLECTION_NAME_PT10M)
                .append("on", listOf(
                    "stz", "st", "ai", "av", "vhi", "t", "vo", "hx", "hy", "hex", "hey",
                    "fevent", "levent"
                ))
                .append("whenMatched", listOf(
                    D("\$addFields", D(
                        "st", "\$st")
                        .append("ai", "\$ai")
                        .append("av", "\$av")
                        .append("vhi", "\$vhi")
                        .append("t", "\$t")
                        .append("vo", "\$vo")
                        .append("hx", "\$hx")
                        .append("hy", "\$hy")
                        .append("hex", "\$hex")
                        .append("hey", "\$hey")
                        .append("stz", "\$\$new.stz")
                        .append("bft", "\$\$new.bft")
                        .append("count", D("\$sum", listOf("\$count", "\$\$new.count")))
                    )
                ))
                .append("whenNotMatched", "insert")
        )
    }

    private fun _getQueryString(query: List<D>): String {
        return "[" + query.joinToString(separator = ",", transform = {
            it.toJson()
        }) + "]"
    }
}
