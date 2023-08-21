package io.userhabit.batch

import io.userhabit.Server
import io.userhabit.batch.common.ExeButler
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import io.userhabit.common.Validator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.name

object BatchUtil {
    private val log = Loggers.getLogger(BatchUtil.javaClass)
    val COLLECTION_NAME = "batch"
    val STATUS_READY = "READY"
    val STATUS_RUNNING = "RUNNING"
    val STATUS_FAILED = "FAILED"
    val STATUS_DONE = "DONE"
    private val collBatch = MongodbUtil.getCollection(COLLECTION_NAME)
    val toDateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")

    fun start() {
        Flux.interval(Duration.ofSeconds(0), Duration.ofMinutes(10))
            .flatMap {
                val zdt = ZonedDateTime.now(ZoneOffset.UTC)
                    .withSecond(0).withNano(0)

                val fromDtMinute = zdt.minusMinutes(10L + (zdt.minute % 10))
                val toDtMinute = fromDtMinute.plusMinutes(10)

                val fromDtHour = zdt.withMinute(0).minusHours(1)
                val toDtHour = fromDtHour.plusHours(1)

                val fromDtDay = zdt.withHour(toDtHour.hour).withMinute(0).minusDays(1)
                val toDtDay = fromDtDay.plusDays(1)
//				println("Now Date: ${zdt.plusHours(9)}\n10Min: ${fromDtMinute.plusHours(9).format(toDateFormater)}, ${toDtMinute.plusHours(9).format(toDateFormater)}  \n1Day: ${fromDtDay.plusHours(9).format(toDateFormater)}, ${toDtDay.plusHours(9).format(toDateFormater)}")
                this._getFluxAllBatches(
                    fromDtMinute,
                    toDtMinute,
                    fromDtDay,
                    toDtDay
                )
            }
            .doOnError { e ->
                log.error("Batch interval error", e)
            }
            .subscribe()
    }

    fun run(
        batchName: Class<Any>,
        query: String,
        srcCollName: String,
        fromDt: ZonedDateTime,
        toDt: ZonedDateTime
    ): Mono<String> {
        val exer = ExeButler()
        return exer.run(batchName, query, srcCollName, fromDt, toDt)
    }

    fun getNameTenMinutes(prefixName: Class<*>): String {
        return "${prefixName.simpleName.replace(Regex("([A-Z])"), "_$1")}_pt10m"
            .substring(1).toLowerCase()
    }

    fun getNameOneHour(prefixName: Class<*>): String {
        return "${prefixName.simpleName.replace(Regex("([A-Z])"), "_$1")}_pt1h"
            .substring(1).toLowerCase()
    }

    fun getNameOneDay(prefixName: Class<*>): String {
        return "${prefixName.simpleName.replace(Regex("([A-Z])"), "_$1")}_pt24h"
            .substring(1).toLowerCase()
    }

    fun _getFluxAllBatches(
        fromDtMinute: ZonedDateTime,
        toDtMinute: ZonedDateTime,
        fromDtDay: ZonedDateTime,
        toDtDay: ZonedDateTime
    ): Flux<Map<String, Any>> {
        return Flux.concat {
            // Notice
            // the below batch processes are moved to 'BatchLaterService' because the long process time
            //
            // - ["HeatmapByViewBatch", "ReachRateByScrollViewBatch", "PreviewByCohort", "AllFlowBatch"]
            //
            //

            // 뷰 리스트
            ViewListBatch.tenMinutes(fromDtMinute, toDtMinute)
                .concatWith(ViewListBatch.oneDay(fromDtDay, toDtDay))
                // 크래시 리스트
                .concatWith(CrashListBatch.tenMinutes(fromDtMinute, toDtMinute))
                // 뷰의 디바이스 수
                .concatWith(DeviceByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(DeviceByViewBatch.oneDay(fromDtDay, toDtDay))
                // 뷰의 크래시 디바이스 수
                .concatWith(DeviceByViewIsCrashBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(DeviceByViewIsCrashBatch.oneDay(fromDtDay, toDtDay))
                // 뷰 수 = viewCount
                .concatWith(ViewByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(ViewByViewBatch.oneDay(fromDtDay, toDtDay))
                // 뷰당 체류시간 = dwellTimeOfView
                .concatWith(DwellTimeByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(DwellTimeByViewBatch.oneDay(fromDtDay, toDtDay))
                // 뷰당 세션 수 = sessionCountOfView
                .concatWith(SessionByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(SessionByViewBatch.oneDay(fromDtDay, toDtDay))
                // 뷰당 크래시 수 = crashCountOfView
                .concatWith(CrashByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(CrashByViewBatch.oneDay(fromDtDay, toDtDay))
                // 뷰당 앱 종료 = sessionEndCountOfView
                .concatWith(AppTerminationByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(AppTerminationByViewBatch.oneDay(fromDtDay, toDtDay))
                // 뷰당 스와이프 이벤트 방향 수 = swipeDirectionCountOfView
                .concatWith(SwipeDirectionByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(SwipeDirectionByViewBatch.oneDay(fromDtDay, toDtDay))
                // 뷰당 이벤트 수 = eventCountOfView
                .concatWith(EventByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(EventByViewBatch.oneDay(fromDtDay, toDtDay)).subscribe()
            // 오브젝트 리스트
            ObjectListBatch.tenMinutes(fromDtMinute, toDtMinute)
                .concatWith(ObjectListBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(SessionByOsBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(SessionByOsBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(SessionByCountryBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(SessionByCountryBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(SessionByDevicenameBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(SessionByDevicenameBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(SessionByResolutionBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(SessionByResolutionBatch.oneDay(fromDtDay, toDtDay))
                // 스크린의 오브젝트 수 합 = objectCountOfScreen
                .concatWith(ObjectByViewBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(ObjectByViewBatch.oneDay(fromDtDay, toDtDay))
                // 개별사용흐름
                .concatWith(EachFlowBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(EachFlowBatch.oneDay(fromDtDay, toDtDay))
                // 개별사용흐름, 오브젝트, 액션 추적 배치
                .concatWith(TrackingFlowBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(TrackingFlowBatch.oneDay(fromDtDay, toDtDay))
                // 전체 화면 흐름 배치
                // @see BatchLaterService
                // .concatWith(AllFlowBatch.tenMinutes(fromDtMinute, toDtMinute))
                // .concatWith(AllFlowBatch.oneDay(fromDtDay, toDtDay))
                .subscribe()
            // 뷰 히트맵 배치
            // @see BatchLaterService
            // HeatmapByViewBatch.tenMinutes(fromDtMinute, toDtMinute)
            //     // 스크롤 뷰 히트맵 배치
            //     .concatWith(HeatmapByScrollViewBatch.tenMinutes(fromDtMinute, toDtMinute))
            //     .concatWith(HeatmapByScrollViewBatch.oneDay(fromDtDay, toDtDay))
            //     // 스크롤 뷰 도달율 배치
            //     .concatWith(ReachRateByScrollViewBatch.tenMinutes(fromDtMinute, toDtMinute))
            //     .concatWith(ReachRateByScrollViewBatch.oneDay(fromDtDay, toDtDay))
            //     // 코호트 미리보기 배치
            //     .concatWith(PreviewByCohort.tenMinutes(fromDtMinute, toDtMinute)).subscribe()
            //
            SessionByAppBatch.tenMinutes(fromDtMinute, toDtMinute)
                .concatWith(SessionByAppBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(DeviceByAppBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(DeviceByAppBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(ViewByAppBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(ViewByAppBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(ViewByAppIsUniqueBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(ViewByAppIsUniqueBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(EventByAppBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(EventByAppBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(DwellTimeByAppBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(DwellTimeByAppBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(SessionDeviceByAppIsCrashBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(SessionDeviceByAppIsCrashBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(SessionByOsDevicenameBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(SessionByOsDevicenameBatch.oneDay(fromDtDay, toDtDay))
                //
                .concatWith(DeviceByOsDevicenameBatch.tenMinutes(fromDtMinute, toDtMinute))
                .concatWith(DeviceByOsDevicenameBatch.oneDay(fromDtDay, toDtDay)).subscribe()
            // ISO 추가 배치
            IpToIsoBatch.tenMinutesAdv(fromDtMinute, toDtMinute).subscribe()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

        Server.logInit()

        val className = System.getProperty("class", "")
        val fromDate = System.getProperty("from_date", "")
        val toDate = System.getProperty("to_date", "")

        val vali = Validator()
            .new(className, "class").required()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()

        if (vali.isNotValid()) {
            val now = ZonedDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            println(Util.toPretty(vali.toExceptionList()))
            println(
                """
				Usage:
				./gradlew batch -Pclass=ObjectListBatch -Pfrom_date=${now.format(formatter)}T00:00:00Z -Pto_date=${
                    now.format(
                        formatter
                    )
                }T01:00:00Z	
			""".trimIndent()
            )
            println("")
        }

        val path = "io/userhabit/batch/"
        val classPath = "${path.replace("/", ".")}${className}"
        try {
            Class.forName(classPath)
        } catch (ex: Throwable) {
            println(
                """
				"${className}" class does not exist
				
				The following is a list of available classes:
			""".trimIndent()
            )
            Util
                .getResourceList("io/userhabit/batch/") {
                    it.name
                }
                .filter { it.contains("Batch.class") }
                .sorted()
                .forEach { println("${it.substringBefore(".")}") }
            return
        }

        val kotlinClass = Class.forName(classPath).kotlin
        val callableMinute = kotlinClass.members.find { it.name == "tenMinutes" }
        val callableDay = kotlinClass.members.find { it.name == "oneDay" }
        if (callableMinute == null || callableDay == null) {
            println(
                """
				"tenMinutes" or "oneDay" function does not exist
				
				The following is a list of available functions:
			""".trimIndent()
            )
            kotlinClass.members.forEach { println(it) }
            return
        }

        val instance = kotlinClass.objectInstance

        val monoMinute =
            callableMinute.call(instance, ZonedDateTime.parse(fromDate), ZonedDateTime.parse(toDate)) as Mono<String>
        monoMinute
            .concatWith(
                callableDay.call(
                    instance,
                    ZonedDateTime.parse(fromDate),
                    ZonedDateTime.parse(toDate)
                ) as Mono<String>
            )
//			.log()
            .blockLast()

        // TODO
//		val tenMinute = Duration.ofMinutes(10).toMillis()
//		val tenMinuteList = (Instant.parse(fromDate).toEpochMilli()..Instant.parse(toDate).toEpochMilli() - tenMinute step tenMinute).toList()
//			.map { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
////			.map { Date(it) }
////			.map { println(it) }
//			.toTypedArray()
//		println(tenMinuteList.first())
//		println(tenMinuteList.last())
//
////		Flux.fromArray(tenMinuteList)
//		Flux.just(tenMinuteList.first())
//			.flatMap {
//				callableMinute!!.call(instance, it, it.plusMinutes(10)) as Mono<String>
//			}
//			.onBackpressureBuffer(10)
////			.delaySubscription(Duration.ofSeconds(2))
//			.map {
//				println(it)
//				it
//			}
//			.blockLast()

    } // end of main()
}
