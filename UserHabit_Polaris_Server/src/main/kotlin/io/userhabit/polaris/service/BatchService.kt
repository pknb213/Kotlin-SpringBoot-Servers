package io.userhabit.polaris.service


import io.userhabit.batch.*
import io.userhabit.common.SafeRequest
import io.userhabit.common.Status
import io.userhabit.polaris.service.batch.BatchLaterServiceButler
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.time.ZoneOffset
import java.time.ZonedDateTime


/**
 * use crontab to run this at night
 */
object BatchService {
    private val log = Loggers.getLogger(this.javaClass)

    fun run(req: SafeRequest): Mono<Map<String, Any>> {

        val zdt = ZonedDateTime.now(ZoneOffset.UTC)
            .withSecond(0).withNano(0)

        val fromDtMinute = zdt.minusMinutes(10L + (zdt.minute % 10))
        val toDtMinute = fromDtMinute.plusMinutes(10)

        val fromDtHour = zdt.withMinute(0).minusHours(1)
        val toDtHour = fromDtHour.plusHours(1)

        val fromDtDay = zdt.withHour(toDtHour.hour).withMinute(0).minusDays(1)
        val toDtDay = fromDtDay.plusDays(1)

        return this._runAllBatchStream(
            fromDtMinute,
            toDtMinute,
            fromDtDay,
            toDtDay
        ).map {
            log.info(it.toString())
            Status.status200Ok(it)
        }

    }

    fun _runAllBatchStream(
        fromDtMinute: ZonedDateTime,
        toDtMinute: ZonedDateTime,
        fromDtDay: ZonedDateTime,
        toDtDay: ZonedDateTime
    ): Mono<List<String>> {


        // 뷰 리스트
        val batchStream1 = ViewListBatch.tenMinutes(fromDtMinute, toDtMinute)
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
            .concatWith(EventByViewBatch.oneDay(fromDtDay, toDtDay))

        // 오브젝트 리스트
        val batchStream2 = ObjectListBatch.tenMinutes(fromDtMinute, toDtMinute)
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
        val batchStream3 = SessionByAppBatch.tenMinutes(fromDtMinute, toDtMinute)
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
            .concatWith(DeviceByOsDevicenameBatch.oneDay(fromDtDay, toDtDay))
        // ISO 추가 배치
        val batchStream4 = IpToIsoBatch.tenMinutesAdv(fromDtMinute, toDtMinute)

        return Flux.merge(batchStream1, batchStream2, batchStream3, batchStream4)
            .collectList()
    }
}
