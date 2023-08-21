package io.userhabit.polaris.service

import io.userhabit.batch.indicators.CrashList
import io.userhabit.batch.indicators.IndicatorSessionDeviceByAppIsCrash
import io.userhabit.common.*
import io.userhabit.common.utils.QueryPeasant
import org.bson.Document as D
import io.userhabit.polaris.Protocol as P
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Instant
import java.util.*

/**
 *  크래시 분석
 *  @author lje
 *  @comment 21.11.18 yj
 *  @sample GET {{localhost}}/v3/crash/{ids}?from_date=2021-01-01T00:00:00Z&to_date=2021-12-31T00:00:00Z&by=stack_trace
 *  @param
 *  	by: stack_trace or Not Use.
 *  	crash_id_list: stack_trace 일 때만 가능.
 *  @return (Default by=stack_trace )
 *  data=[{
		"session_id": "0_cf9f99b08d6744d9bd3fac192538c3c7",
		"crash_stacktrace": "java.lang.IllegalArgumentException: parameter must be a descendant of this view\\n\tat android.view.ViewGroup.offsetRectBetweenParentAndChild(ViewGroup.java:6329)\\n\tat android.view.ViewGroup.offsetDescendantRectToMyCoords(ViewGroup.java:6251)\\n\tat android.view.FocusFinder$FocusSorter.sort(FocusFinder.java:834)\\n\tat android.view.FocusFinder.sort(FocusFinder.java:875)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1295)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1297)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1297)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1297)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1297)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1297)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1297)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1297)\\n\tat android.view.ViewGroup.addFocusables(ViewGroup.java:1297)\\n\tat android.view.View.addFocusables(View.java:11965)\\n\tat android.view.FocusFinder.findNextFocus(FocusFinder.java:108)\\n\tat android.view.FocusFinder.findNextFocus(FocusFinder.java:80)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1028)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.ViewGroup.focusSearch(ViewGroup.java:1030)\\n\tat android.view.View.focusSearch(View.java:11630)\\n\tat android.widget.TextView.onCreateInputConnection(TextView.java:8917)\\n\tat android.view.inputmethod.InputMethodManager.startInputInner(InputMethodManager.java:1763)\\n\tat android.view.inputmethod.InputMethodManager.checkFocus(InputMethodManager.java:2114)\\n\tat android.view.ViewRootImpl$ViewRootHandler.handleMessage(ViewRootImpl.java:4992)\\n\tat android.os.Handler.dispatchMessage(Handler.java:106)\\n\tat android.os.Looper.loop(Looper.java:214)\\n\tat android.app.ActivityThread.main(ActivityThread.java:6986)\\n\tat java.lang.reflect.Method.invoke(Native Method)\\n\tat com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:494)\\n\tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1445)\\n",
		"crash_type": "IllegalArgumentException",
		"crash_message": "parameter must be a descendant of this view"
 * }, ]
 * @return (by=Null)
 * 	data = [{
 * 		"app_version": "1.0.1",
 *		"crash_id": -526063915,
 *		"crash_type": "IndexOutOfBoundsException",
 *		"crash_message": "Index: 100, Size: 0",
 *		"crash_session_count": 5,
 *		"crash_device_count": 4
 * 	}]
 */
object CrashService {
	private val log = Loggers.getLogger(this.javaClass)
	// @deprecated
	// lateinit var FIELD_LIST_MAP: Map<String, List<Map<String, String>>>

	fun get(req:SafeRequest): Mono<Map<String, Any>> {
		val appIdList = req.splitQueryOrDefault("app_id_list", "")
		val ids = req.splitParamOrDefault("ids","")

		val versionList = req.splitQueryOrDefault("version_list", "")
		val fromDate = req.getQueryOrDefault("from_date", "")
		val toDate = req.getQueryOrDefault("to_date", "")
		val crashIdList = req.splitQueryOrDefault("crash_id_list", "")
		val by = req.getQueryOrDefault("by", "")
		val osVersion = req.splitQueryOrDefault("os_version", "")
		val deviceName = req.splitQueryOrDefault("device_name", "")
		val skip = req.getQueryOrDefault("skip", 0)
		val limit = req.getQueryOrDefault("limit", 40)
		val fieldList = req.splitQueryOrDefault("field_list", "")

		val vali = Validator()
			.new(fromDate, "from_date").required().date()
			.new(toDate, "to_date").required().date()
			.new(limit, "limit").max(100)

		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return AppService.getAppIdList(P.uiK, req.getUserId())
			.flatMap { ownedAppIdList ->
				val appIdList = req.splitQueryOrDefault("app_id_list", "").let { ailParam ->
					if (ailParam.isNotEmpty()) ailParam.filter { ownedAppIdList.contains(it) } else ownedAppIdList
				}
				val appIdInObjIds = QueryPeasant.convertToObjectIdList(appIdList)

				val (query, coll) = if (by == "stack_trace") {
					val projectDoc = D().append("_id", 0)
						.append(P.siK, "\$${P.si}")
						.append(P.csK, "\$${P.cs}")
						.append(P.ctK, "\$${P.ct}")
						.append(P.cmK, "\$${P.cm}")

					val matchQuery = D()
						.append(P.stz, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))) )
						.append(P.ai, D("\$in", appIdInObjIds))

					if(versionList.isNotEmpty()) {
						matchQuery.append(P.av, D("\$in", versionList))
					}

					if(deviceName.isNotEmpty()) {
						matchQuery.append(P.dn, D("\$in", deviceName))
					}

					if(osVersion.isNotEmpty()) {
						matchQuery.append(P.dov, D("\$in", osVersion))
					}

					if(crashIdList.isNotEmpty()) {
						matchQuery.append(P.ci, D("\$in", crashIdList.map { it.toLong() }))
					}

					Pair(
						listOf(
							D("\$match", matchQuery),
							D("\$project", projectDoc),
							D("\$skip", skip),
							D("\$limit", limit)
						),
						CrashList.COLLECTION_NAME_PT10M
					)
				} else {
					// type list
					val matchQuery = D()
						.append(P.stz, D("\$gte", Date.from(Instant.parse(fromDate))).append("\$lte", Date.from(Instant.parse(toDate))) )
						.append(P.ai, D("\$in", appIdInObjIds))

					if(versionList.isNotEmpty()) {
						matchQuery.append(P.av, D("\$in", versionList))
					}

					if(deviceName.isNotEmpty()) {
						matchQuery.append(P.dn, D("\$in", deviceName))
					}

					if(osVersion.isNotEmpty()) {
						matchQuery.append(P.dov, D("\$in", osVersion))
					}

					val projectDoc = D().append("_id", 0)
						.append(P.avK, "\$_id.${P.av}")
						.append(P.ciK, "\$_id.${P.ci}")
						.append(P.ctK, "\$_id.${P.ct}")
						.append(P.cmK, "\$_id.${P.cm}")
						.append(P.cscoK, "\$${P.csco}")
						.append(P.cdcoK, "\$${P.cdco}")

					Pair(
						listOf(
							D("\$match", matchQuery),
							D("\$lookup", D()
								.append("from", CrashList.COLLECTION_NAME_PT10M)
								.append("let", D().append(P.ci, "\$${P.ci}"))
								.append("pipeline", listOf(
									D("\$match", D("\$expr", D("\$and", listOf(
										D("\$eq", listOf("\$${P.ci}", "\$\$${P.ci}"))
									)))),
									D("\$group", D()
										.append("_id", D()
											.append(P.ct, "\$${P.ct}")
											.append(P.ci, "\$${P.ci}")
											.append(P.cm, "\$${P.cm}"))
									)))
								.append("as", "crash")),
							D("\$unwind", "\$crash"),
							D("\$group", D()
								.append("_id", D()
									.append(P.av, "\$${P.av}")
									.append(P.ci, "\$${P.ci}")
									.append(P.ct, "\$crash._id.${P.ct}")
									.append(P.cm, "\$crash._id.${P.cm}")
								)
								.append(P.csco, D("\$sum", "\$${P.csco}"))
								.append(P.cdco, D("\$sum", "\$${P.cdco}"))
							),
							D("\$project", projectDoc),
							D("\$skip", skip),
							D("\$limit", limit)
						),
						IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H
					)
				}

				Flux
					.from(MongodbUtil.getCollection(coll)
						.aggregate(query)
					)
					.collectList()
					.doOnError {
						log.info(Util.toPretty(query))
					}
					.map {
						Status.status200Ok(it)
					}
			}
	} // end of get()

}
