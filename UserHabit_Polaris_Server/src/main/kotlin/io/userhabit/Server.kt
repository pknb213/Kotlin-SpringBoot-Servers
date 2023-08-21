package io.userhabit

import ch.qos.logback.classic.Level
<<<<<<< HEAD
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import io.userhabit.anejo.service.ServiceHelperAnejo
import io.userhabit.anejo.service.CloseService
import io.userhabit.anejo.service.OpenService
import io.userhabit.anejo.service.ScreenShotService
=======
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import io.userhabit.anejo.service.CloseService
import io.userhabit.anejo.service.OpenService
import io.userhabit.anejo.service.ScreenShotService
import io.userhabit.anejo.service.ServiceHelperAnejo
>>>>>>> master
import io.userhabit.common.Config
import io.userhabit.polaris.service.StorageService
import io.userhabit.common.MongodbUtil
import io.userhabit.common.Util
import io.userhabit.polaris.service.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.util.Loggers
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
<<<<<<< HEAD
import kotlin.io.path.absolutePathString
=======
>>>>>>> master

object Server {
    private val log = Loggers.getLogger(this.javaClass)

    init {
//    Runtime.getRuntime.exec(s"kill ${ProcessHandle.current()}")
<<<<<<< HEAD
		logInit()
	}

	@JvmStatic
	fun main(args: Array<String>) {

		log.debug(System.getProperties().map { it.toString().plus("\n") }.sorted().toString())
		log.info("hostname : ${Util.hostName}")

		args.map { it.split("=").let { System.setProperty(it[0], it.getOrNull(1)) } }
		val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
		logger.level = if(System.getProperty("log.level") == "debug") Level.DEBUG else Level.INFO

		val httpPort = Config.getInt("http.port")
		val server = HttpServer.create()
			.route {
				val staticFilePath = Paths.get(Config.get("static.file_path"))
				if(!Files.isDirectory(staticFilePath)) log.error("Directory not found [${staticFilePath}]")
				it
					.ws("/v3", ServiceHelper::websocket)

					.get("/ping") {req, resp -> resp.sendString(Mono.just("pong\n"))}

					.post("/v3/session_event", ServiceHelper.http(SessionEventService::postAndPut, false))

					.get("/v3/session/count/of-each-view",ServiceHelper.http(SessionService::getCountOfEachScreen))
					.get("/v3/session/count/of-device-id",ServiceHelper.http(SessionService::getCountOfDeviceId))
					.get("/v3/session/last-session",ServiceHelper.http(SessionService::getTheLast))
					.get("/v3/session/first-session",ServiceHelper.http(SessionService::getTheFirst))

					.get("/v3/session/{ids}",ServiceHelper.http(SessionService::get))
					.get("/v3/session/{ids}/csv",ServiceHelper.http(SessionService::get))
					.get("/v3/session/{ids}/crash",ServiceHelper.http(SessionService::crash))
					.get("/v3/session/{ids}/count",ServiceHelper.http(SessionService::count))
					.get("/v3/session/{ids}/rank",ServiceHelper.http(SessionService::rank))
					.get("/v3/session/{ids}/open",ServiceHelper.http(SessionService::open, false))
//					.delete("/v3/session/{ids}",ServiceHelper.http(SessionService::delete))
//					.post  ("/v3/session",ServiceHelper.http(SessionService::postAndPut, false))

					.get("/v3/event/of-session",ServiceHelper.http(EventService::getEventsOfSession))
					.get("/v3/event/{ids}",ServiceHelper.http(EventService::get))
					.get("/v3/event/{ids}/csv",ServiceHelper.http(EventService::get))
					.get("/v3/event/{ids}/count",ServiceHelper.http(EventService::count))
					.get("/v3/event/{ids}/rank",ServiceHelper.http(EventService::rank))
//					.delete("/v3/event/{ids}",ServiceHelper.http(EventService::delete))
//					.post  ("/v3/event",ServiceHelper.http(EventService::postAndPut, false))

					.get("/v3/member/{ids}",ServiceHelper.http(MemberService::get))
					.get("/v3/member/{ids}/csv",ServiceHelper.http(MemberService::get))
					.get("/v3/member/{ids}/count",ServiceHelper.http(MemberService::count))
					.post("/v3/member",ServiceHelper.http(MemberService::post, false))
					.post("/v3/member/login",ServiceHelper.http(MemberService::login, false))
					.post("/v3/member/auth_email",ServiceHelper.http(MemberService::authEmail, false))
					.post("/v3/member/invite",ServiceHelper.http(MemberService::invite))
					.put("/v3/member",ServiceHelper.http(MemberService::put))
					.delete("/v3/member/{ids}",ServiceHelper.http(MemberService::delete))

					.get("/v3/view/count/total",ServiceHelper.http(ViewService::getTotalCount))
					.get("/v3/view/count/of-list",ServiceHelper.http(ViewService::getViewCountOfList))
					.get("/v3/view/list",ServiceHelper.http(ViewService::getViewList))
					.get("/v3/view/{ids}",ServiceHelper.http(ViewService::get))
					.get("/v3/view/{ids}/csv",ServiceHelper.http(ViewService::get))
					.get("/v3/view/{ids}/count",ServiceHelper.http(ViewService::count))
					.get("/v3/view/{ids}/rank",ServiceHelper.http(ViewService::rank))

					.get("/v3/object/{ids}",ServiceHelper.http(ObjectService::get))
					.get("/v3/object/{ids}/csv",ServiceHelper.http(ObjectService::get))
					.get("/v3/object/{ids}/count",ServiceHelper.http(ObjectService::count))

					.get("/v3/config/of-app", ServiceHelper.http(AppService::getAppConfig))
					.post("/v3/config/of-app/{appIdList}", ServiceHelper.http(AppService::postAppConfig))
					.get("/v3/app/stats-session-view-dwell",ServiceHelper.http(AppService::getStatsSessionViewDwell))
					.get("/v3/app/{ids}",ServiceHelper.http(AppService::get))
					.get("/v3/app/{ids}/csv",ServiceHelper.http(AppService::get))
					.get("/v3/app/{ids}/count",ServiceHelper.http(AppService::count))
					.get("/v3/app/{ids}/config", ServiceHelper.http(AppService::getConfig, isAuth = false))
					.post("/v3/app/{ids}/config", ServiceHelper.http(AppService::postConfig))
					.post("/v3/app",ServiceHelper.http(AppService::postAndPut))
					.put("/v3/app",ServiceHelper.http(AppService::postAndPut))
					.delete("/v3/app",ServiceHelper.http(AppService::delete))

					.get("/v3/device/{ids}/count",ServiceHelper.http(DeviceService::count))
					.get("/v3/device/{ids}/rank",ServiceHelper.http(DeviceService::rank))

					.get("/v3/dwell_time/{ids}/count",ServiceHelper.http(DwellTimeService::count))
					.get("/v3/dwell_time/{ids}/rank",ServiceHelper.http(DwellTimeService::rank))

					.get("/v3/crash/{ids}",ServiceHelper.http(CrashService::get))
					.get("/v3/crash/{ids}/csv",ServiceHelper.http(CrashService::get))

					.get("/v3/flow/target-flow-of-depth",ServiceHelper.http(FlowService::getTargetFlowOfDepth))
					.get("/v3/flow/eachflow-graph-nodes",ServiceHelper.http(FlowService::getNodesOfEachFlowGraph))
					.get("/v3/flow/eachflow-graph-edges",ServiceHelper.http(FlowService::getEdgesOfEachFlowGraph))
					.get("/v3/flow/{ids}",ServiceHelper.http(FlowService::get))
					.get("/v3/flow/{ids}/csv",ServiceHelper.http(FlowService::get))
					.get("/v3/flow/{ids}/count",ServiceHelper.http(FlowService::count))

//					.get("/v3/replay",ServiceHelper.http(ReplayService::get))
					.get("/v3/replay/list",ServiceHelper.http(ReplayService::getReplayList))
					.get("/v3/replay/{ids}",ServiceHelper.http(ReplayService::get))
					.get("/v3/replay/{ids}/csv",ServiceHelper.http(ReplayService::get))


//					.get("/v3/heatmap/{ids}",ServiceHelper.http(HeatmapService::get))
					.get("/v3/heatmap/of-action-firstlast",ServiceHelper.http(HeatmapService::getFirstLastTapHeatmap))
					.get("/v3/heatmap/of-scrollview",ServiceHelper.http(HeatmapService::getScrollViewHeatmap))
					.get("/v3/heatmap/of-reachrate",ServiceHelper.http(HeatmapService::getReachRateHeatmap))
					.get("/v3/heatmap/{ids}/count",ServiceHelper.http(HeatmapService::count))

					.get("/v3/storage/{ids}",ServiceHelper.http(StorageService::get, false))
					.get("/v3/storage/{ids}/csv",ServiceHelper.http(StorageService::get, false))
					.post("/v3/storage",ServiceHelper.http(StorageService::postAndPut, false))
					.put("/v3/storage",ServiceHelper.http(StorageService::postAndPut, false))
					.directory(StorageService.URI, Path.of(StorageService.PATH))

					.get("/v3/country/{ids}",ServiceHelper.http(CountryService::get, false))
					.get("/v3/country/{ids}/csv",ServiceHelper.http(CountryService::get, false))

					.get("/v3/login_history/{ids}",ServiceHelper.http(LoginHistoryService::get))
					.get("/v3/login_history/{ids}/csv",ServiceHelper.http(LoginHistoryService::get))

					.get("/v3/plan/{ids}",ServiceHelper.http(PlanService::get))
					.get("/v3/plan/{ids}/csv",ServiceHelper.http(PlanService::get))
					.post("/v3/plan",ServiceHelper.http(PlanService::postAndPut))
					.put("/v3/plan",ServiceHelper.http(PlanService::postAndPut))
					.delete("/v3/plan/{ids}",ServiceHelper.http(PlanService::delete))

					.get("/v3/plan_history/{ids}",ServiceHelper.http(PlanHistoryService::get))
					.get("/v3/plan_history/{ids}/csv",ServiceHelper.http(PlanHistoryService::get))
					.post("/v3/plan_history",ServiceHelper.http(PlanHistoryService::post))

					.get("/v3/payment_history/{ids}",ServiceHelper.http(PaymentHistoryService::get))
					.get("/v3/payment_history/{ids}/csv",ServiceHelper.http(PaymentHistoryService::get))
					.post("/v3/payment_history",ServiceHelper.http(PaymentHistoryService::post))

					.get("/v3/company/{ids}",ServiceHelper.http(CompanyService::get))
					.get("/v3/company/{ids}/csv",ServiceHelper.http(CompanyService::get))
					.post("/v3/company",ServiceHelper.http(CompanyService::postAndPut))
					.put("/v3/company",ServiceHelper.http(CompanyService::postAndPut))
					.put("/v3/company/{ids}/plan",ServiceHelper.http(CompanyService::putPlan))
					.delete("/v3/company/{ids}",ServiceHelper.http(CompanyService::delete))

					.get("/v3/bookmark", ServiceHelper.http(BookmarkService::get))
					.get("/v3/bookmark/{ids}/csv", ServiceHelper.http(BookmarkService::get))
					.post("/v3/bookmark", ServiceHelper.http(BookmarkService::postAndPut))

//					.get("/v3/alias", ServiceHelper.http(AliasService::get))
					.post("/v3/alias", ServiceHelper.http(AliasService::postAndPut))

					.get("/v3/notice/{ids}",ServiceHelper.http(NoticeService::get, false))
					.get("/v3/notice/{ids}/csv",ServiceHelper.http(NoticeService::get, false))
					.post("/v3/notice",ServiceHelper.http(NoticeService::postAndPut))
					.put("/v3/notice",ServiceHelper.http(NoticeService::postAndPut))

					.get("/v3/qna/{ids}",ServiceHelper.http(QnaService::get, false))
					.get("/v3/qna/{ids}/csv",ServiceHelper.http(QnaService::get, false))
					.post("/v3/qna",ServiceHelper.http(QnaService::postAndPut))
					.put("/v3/qna",ServiceHelper.http(QnaService::postAndPut))

					.get("/v3/geo", ServiceHelper.http(GeoService::get, false))
					.get("/v3/geo/session-country-code", ServiceHelper.http(GeoService::updateSessionCountryCode, false))
=======
        logInit()
    }

    @JvmStatic
    fun main(args: Array<String>) {

        log.debug(System.getProperties().map { it.toString().plus("\n") }.sorted().toString())
        log.info("hostname : ${Util.hostName}")

        args.map { it.split("=").let { System.setProperty(it[0], it.getOrNull(1)) } }

        this._setLogger()

        val httpPort = Config.getInt("http.port")
        val server = HttpServer.create()
            .route {
                val staticFilePath = Paths.get(Config.get("static.file_path"))
                if (!Files.isDirectory(staticFilePath)) log.error("Directory not found [${staticFilePath}]")
                it
                    .ws("/v3", ServiceHelper::websocket)

                    .get("/ping") { req, resp -> resp.sendString(Mono.just("pong\n")) }

                    .post("/v3/session_event", ServiceHelper.http(SessionEventService::postAndPut, false))

                    .get("/v3/session/count/of-each-view", ServiceHelper.http(SessionService::getCountOfEachScreen))
                    .get("/v3/session/count/of-device-id", ServiceHelper.http(SessionService::getCountOfDeviceId))
                    .get("/v3/session/last-session", ServiceHelper.http(SessionService::getTheLast))
                    .get("/v3/session/first-session", ServiceHelper.http(SessionService::getTheFirst))

                    .get("/v3/session/{ids}", ServiceHelper.http(SessionService::get))
                    .get("/v3/session/{ids}/csv", ServiceHelper.http(SessionService::get))
                    .get("/v3/session/{ids}/crash", ServiceHelper.http(SessionService::crash))
                    .get("/v3/session/{ids}/count", ServiceHelper.http(SessionService::count))
                    .get("/v3/session/{ids}/rank", ServiceHelper.http(SessionService::rank))
                    .get("/v3/session/{ids}/open", ServiceHelper.http(SessionService::open, false))
//					.delete("/v3/session/{ids}",ServiceHelper.http(SessionService::delete))
//					.post  ("/v3/session",ServiceHelper.http(SessionService::postAndPut, false))

                    .get("/v3/event/of-session", ServiceHelper.http(EventService::getEventsOfSession))
                    .get("/v3/event/{ids}", ServiceHelper.http(EventService::get))
                    .get("/v3/event/{ids}/csv", ServiceHelper.http(EventService::get))
                    .get("/v3/event/{ids}/count", ServiceHelper.http(EventService::count))
                    .get("/v3/event/{ids}/rank", ServiceHelper.http(EventService::rank))
//					.delete("/v3/event/{ids}",ServiceHelper.http(EventService::delete))
//					.post  ("/v3/event",ServiceHelper.http(EventService::postAndPut, false))

                    .get("/v3/member/{ids}", ServiceHelper.http(MemberService::get))
                    .get("/v3/member/{ids}/csv", ServiceHelper.http(MemberService::get))
                    .get("/v3/member/{ids}/count", ServiceHelper.http(MemberService::count))
                    .post("/v3/member", ServiceHelper.http(MemberService::post, false))
                    .post("/v3/member/login", ServiceHelper.http(MemberService::login, false))
                    .post("/v3/member/auth_email", ServiceHelper.http(MemberService::authEmail, false))
                    .post("/v3/member/invite", ServiceHelper.http(MemberService::invite))
                    .put("/v3/member", ServiceHelper.http(MemberService::put))
                    .delete("/v3/member/{ids}", ServiceHelper.http(MemberService::delete))

                    .get("/v3/view/count/total", ServiceHelper.http(ViewService::getTotalCount))
                    .get("/v3/view/count/of-list", ServiceHelper.http(ViewService::getViewCountOfList))
                    .get("/v3/view/list", ServiceHelper.http(ViewService::getViewList))
                    .get("/v3/view/{ids}", ServiceHelper.http(ViewService::get))
                    .get("/v3/view/{ids}/csv", ServiceHelper.http(ViewService::get))
                    .get("/v3/view/{ids}/count", ServiceHelper.http(ViewService::count))
                    .get("/v3/view/{ids}/rank", ServiceHelper.http(ViewService::rank))

                    .get("/v3/object/{ids}", ServiceHelper.http(ObjectService::get))
                    .get("/v3/object/{ids}/csv", ServiceHelper.http(ObjectService::get))
                    .get("/v3/object/{ids}/count", ServiceHelper.http(ObjectService::count))

                    .get("/v3/config/of-app", ServiceHelper.http(AppService::getAppConfig))
                    .post("/v3/config/of-app/{appIdList}", ServiceHelper.http(AppService::postAppConfig))
                    .get("/v3/app/stats-session-view-dwell", ServiceHelper.http(AppService::getStatsSessionViewDwell))
                    .get("/v3/app/{ids}", ServiceHelper.http(AppService::get))
                    .get("/v3/app/{ids}/csv", ServiceHelper.http(AppService::get))
                    .get("/v3/app/{ids}/count", ServiceHelper.http(AppService::count))
                    .get("/v3/app/{ids}/config", ServiceHelper.http(AppService::getConfig, isAuth = false))
                    .post("/v3/app/{ids}/config", ServiceHelper.http(AppService::postConfig))
                    .post("/v3/app", ServiceHelper.http(AppService::postAndPut))
                    .put("/v3/app", ServiceHelper.http(AppService::postAndPut))
                    .delete("/v3/app", ServiceHelper.http(AppService::delete))

                    .get("/v3/device/{ids}/count", ServiceHelper.http(DeviceService::count))
                    .get("/v3/device/{ids}/rank", ServiceHelper.http(DeviceService::rank))

                    .get("/v3/dwell_time/{ids}/count", ServiceHelper.http(DwellTimeService::count))
                    .get("/v3/dwell_time/{ids}/rank", ServiceHelper.http(DwellTimeService::rank))

                    .get("/v3/crash/{ids}", ServiceHelper.http(CrashService::get))
                    .get("/v3/crash/{ids}/csv", ServiceHelper.http(CrashService::get))

                    .get("/v3/flow/target-flow-of-depth", ServiceHelper.http(FlowService::getTargetFlowOfDepth))
                    .get("/v3/flow/eachflow-graph-nodes", ServiceHelper.http(FlowService::getNodesOfEachFlowGraph))
                    .get("/v3/flow/eachflow-graph-edges", ServiceHelper.http(FlowService::getEdgesOfEachFlowGraph))
                    .get("/v3/flow/{ids}", ServiceHelper.http(FlowService::get))
                    .get("/v3/flow/{ids}/csv", ServiceHelper.http(FlowService::get))
                    .get("/v3/flow/{ids}/count", ServiceHelper.http(FlowService::count))

//					.get("/v3/replay",ServiceHelper.http(ReplayService::get))
                    .get("/v3/replay/list", ServiceHelper.http(ReplayService::getReplayList))
                    .get("/v3/replay/{ids}", ServiceHelper.http(ReplayService::get))
                    .get("/v3/replay/{ids}/csv", ServiceHelper.http(ReplayService::get))


//					.get("/v3/heatmap/{ids}",ServiceHelper.http(HeatmapService::get))
                    .get("/v3/heatmap/of-action-firstlast", ServiceHelper.http(HeatmapService::getFirstLastTapHeatmap))
                    .get("/v3/heatmap/of-scrollview", ServiceHelper.http(HeatmapService::getScrollViewHeatmap))
                    .get("/v3/heatmap/of-reachrate", ServiceHelper.http(HeatmapService::getReachRateHeatmap))
                    .get("/v3/heatmap/{ids}/count", ServiceHelper.http(HeatmapService::count))

                    .get("/v3/storage/{ids}", ServiceHelper.http(StorageService::get, false))
                    .get("/v3/storage/{ids}/csv", ServiceHelper.http(StorageService::get, false))
                    .post("/v3/storage", ServiceHelper.http(StorageService::postAndPut, false))
                    .put("/v3/storage", ServiceHelper.http(StorageService::postAndPut, false))
                    .directory(StorageService.URI, Path.of(StorageService.PATH))

                    .get("/v3/country/{ids}", ServiceHelper.http(CountryService::get, false))
                    .get("/v3/country/{ids}/csv", ServiceHelper.http(CountryService::get, false))

                    .get("/v3/login_history/{ids}", ServiceHelper.http(LoginHistoryService::get))
                    .get("/v3/login_history/{ids}/csv", ServiceHelper.http(LoginHistoryService::get))

                    .get("/v3/plan/{ids}", ServiceHelper.http(PlanService::get))
                    .get("/v3/plan/{ids}/csv", ServiceHelper.http(PlanService::get))
                    .post("/v3/plan", ServiceHelper.http(PlanService::postAndPut))
                    .put("/v3/plan", ServiceHelper.http(PlanService::postAndPut))
                    .delete("/v3/plan/{ids}", ServiceHelper.http(PlanService::delete))

                    .get("/v3/plan_history/{ids}", ServiceHelper.http(PlanHistoryService::get))
                    .get("/v3/plan_history/{ids}/csv", ServiceHelper.http(PlanHistoryService::get))
                    .post("/v3/plan_history", ServiceHelper.http(PlanHistoryService::post))

                    .get("/v3/payment_history/{ids}", ServiceHelper.http(PaymentHistoryService::get))
                    .get("/v3/payment_history/{ids}/csv", ServiceHelper.http(PaymentHistoryService::get))
                    .post("/v3/payment_history", ServiceHelper.http(PaymentHistoryService::post))

                    .get("/v3/company/{ids}", ServiceHelper.http(CompanyService::get))
                    .get("/v3/company/{ids}/csv", ServiceHelper.http(CompanyService::get))
                    .post("/v3/company", ServiceHelper.http(CompanyService::postAndPut))
                    .put("/v3/company", ServiceHelper.http(CompanyService::postAndPut))
                    .put("/v3/company/{ids}/plan", ServiceHelper.http(CompanyService::putPlan))
                    .delete("/v3/company/{ids}", ServiceHelper.http(CompanyService::delete))

                    .get("/v3/bookmark", ServiceHelper.http(BookmarkService::get))
                    .get("/v3/bookmark/{ids}/csv", ServiceHelper.http(BookmarkService::get))
                    .post("/v3/bookmark", ServiceHelper.http(BookmarkService::postAndPut))

//					.get("/v3/alias", ServiceHelper.http(AliasService::get))
                    .post("/v3/alias", ServiceHelper.http(AliasService::postAndPut))

                    .get("/v3/notice/{ids}", ServiceHelper.http(NoticeService::get, false))
                    .get("/v3/notice/{ids}/csv", ServiceHelper.http(NoticeService::get, false))
                    .post("/v3/notice", ServiceHelper.http(NoticeService::postAndPut))
                    .put("/v3/notice", ServiceHelper.http(NoticeService::postAndPut))

                    .get("/v3/qna/{ids}", ServiceHelper.http(QnaService::get, false))
                    .get("/v3/qna/{ids}/csv", ServiceHelper.http(QnaService::get, false))
                    .post("/v3/qna", ServiceHelper.http(QnaService::postAndPut))
                    .put("/v3/qna", ServiceHelper.http(QnaService::postAndPut))

                    .get("/v3/geo", ServiceHelper.http(GeoService::get, false))
                    .get(
                        "/v3/geo/session-country-code",
                        ServiceHelper.http(GeoService::updateSessionCountryCode, false)
                    )
>>>>>>> master
//					.post("/v3/geo", ServiceHelper.http(GeoService::postAndPut, false))

					.get("/v3/batch/do-later", ServiceHelper.http(BatchLaterService::doLater, false))
					.get("/v3/batch/do", ServiceHelper.http(BatchService::run, false))
					// Todo : Batch 테스트
					.get("/v3/test", ServiceHelper.http(BatchTestService::dwellRefac))
					.get("/v3/heatmap/{ids}", ServiceHelper.http(BatchTestService::heatmapTest,false))
					// ########## Console static files ##########
					.index() {req, resp ->
						val p = Paths.get("${staticFilePath}${req.fullPath()}", "/index.html")
						if(Files.exists(p)) resp.compression(true).sendFile(p)
						else resp
							.status(404)
							.sendString(Mono.just("404 NOT FOUND\n"))
//							.sendFile(Paths.get(staticFilePath.toString(), "/404.html")) // TODO create nuxt layout/404.vue
<<<<<<< HEAD
					}
					.directory("/",  staticFilePath)

					// ########## Anejo legacy ##########
					.post("/v2/a/{apikey}/s/{deviceId}/{cuuid}/c", ServiceHelperAnejo.http(CloseService::post))
					.post("/v2/a/{apikey}/s/{deviceId}/{cuuid}", ServiceHelperAnejo.http(OpenService::post))
					.post("/v2/a/{apikey}/g", ServiceHelperAnejo.http(ScreenShotService::getScreenshot)) // 테스트 모드일때 수집된 스크린샷 정보 요청
					.post("/v2/a/{apikey}/v", ServiceHelperAnejo.http(ScreenShotService::postView))
					.post("/v2/a/{apikey}/o", ServiceHelperAnejo.http(ScreenShotService::postObject))
					.post("/v2/a/{apikey}/ss",ServiceHelperAnejo.http(ScreenShotService::postScroll))

			}
			.port(httpPort)
			.compress(true)
			.bindNow()

		if (Config.isDevMode) {
			println("\u001b[35m")
			println("┏━━━━━┓ ┏━━━━┓ ┏┓   ┏┓")
			println("┗┓┏━━┓┃ ┃┏━━━┛ ┃┗┓ ┏┛┃")
			println(" ┃┃  ┃┃ ┃┗━━┓  ┗┓┃ ┃┏┛")
			println(" ┃┃  ┃┃ ┃┏━━┛   ┃┗━┛┃ ")
			println("┏┛┗━━┛┃ ┃┗━━━┓  ┗┓ ┏┛ ")
			println("┗━━━━━┛ ┗━━━━┛   ┗━┛  ")
			println("\u001b[0m")
		}

		MongodbUtil.toString() // for init function()

		log.info("Polaris server running on port ${httpPort}")
		server.onDispose().block()
		log.info("disposNow@@@@@@@@@")
=======
                    }
                    .directory("/", staticFilePath)

                    // ########## Anejo legacy ##########
                    .post("/v2/a/{apikey}/s/{deviceId}/{cuuid}/c", ServiceHelperAnejo.http(CloseService::post))
                    .post("/v2/a/{apikey}/s/{deviceId}/{cuuid}", ServiceHelperAnejo.http(OpenService::post))
                    .post(
                        "/v2/a/{apikey}/g",
                        ServiceHelperAnejo.http(ScreenShotService::getScreenshot)
                    ) // 테스트 모드일때 수집된 스크린샷 정보 요청
                    .post("/v2/a/{apikey}/v", ServiceHelperAnejo.http(ScreenShotService::postView))
                    .post("/v2/a/{apikey}/o", ServiceHelperAnejo.http(ScreenShotService::postObject))
                    .post("/v2/a/{apikey}/ss", ServiceHelperAnejo.http(ScreenShotService::postScroll))

            }
            .port(httpPort)
            .compress(true)
            .bindNow()

        if (Config.isDevMode) {
            println("\u001b[35m")
            println("┏━━━━━┓ ┏━━━━┓ ┏┓   ┏┓")
            println("┗┓┏━━┓┃ ┃┏━━━┛ ┃┗┓ ┏┛┃")
            println(" ┃┃  ┃┃ ┃┗━━┓  ┗┓┃ ┃┏┛")
            println(" ┃┃  ┃┃ ┃┏━━┛   ┃┗━┛┃ ")
            println("┏┛┗━━┛┃ ┃┗━━━┓  ┗┓ ┏┛ ")
            println("┗━━━━━┛ ┗━━━━┛   ┗━┛  ")
            println("\u001b[0m")
        }

        MongodbUtil.toString() // for init function()

        log.info("Polaris server running on port ${httpPort}")
        server.onDispose().block()
        log.info("disposNow@@@@@@@@@")
>>>>>>> master
//      .block(Duration.ofSeconds(10))
    } // end of main method

    fun logInit() {
        // TODO delete (for log4j)
//		Configurator.setRootLevel(if(Util.isDevMode ) Level.DEBUG else Level.INFO)
//		Loggers.resetLoggerFactory()

        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        logger.level = Level.INFO
//		logger.isAdditive = true
        (logger.getAppender("console") as ConsoleAppender).let { app ->
            app.encoder = PatternLayoutEncoder().apply {
//				pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %highlight(%-5level) %logger{36} - %msg%n"
                pattern = "%d{HH:mm:ss} [%thread] %highlight(%-5level) %logger{36} - %msg%n"
                context = app.context
                start()
            }
        }

//		(LoggerFactory.getLogger("org.mongodb.driver.protocol.command") as ch.qos.logback.classic.Logger).let {
//			it.level = Level.DEBUG
//		}

//		private static Logger createLoggerFor(String string, String file) {
//			LoggerContext lc =(LoggerContext) LoggerFactory . getILoggerFactory ();
//			PatternLayoutEncoder ple = new PatternLayoutEncoder();
//
//			ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
//			ple.setContext(lc);
//			ple.start();
//			FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
//			fileAppender.setFile(file);
//			fileAppender.setEncoder(ple);
//			fileAppender.setContext(lc);
//			fileAppender.start();
//
//			Logger logger =(Logger) LoggerFactory . getLogger (string);
//			logger.addAppender(fileAppender);
//			logger.setLevel(Level.DEBUG);
//			logger.setAdditive(false); /* set to true if root should log too */
//
//			return logger;
//		}
    }

    fun _setLogger(){
        val logLevel = System.getProperty("log.level")
        val logDir = Config.get("log.directory")

        // @ref: https://logback.qos.ch/manual/layouts.html
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        root.level = if(logLevel == "debug") Level.DEBUG else Level.INFO

        val logEncoder = PatternLayoutEncoder()
        logEncoder.context = loggerContext
        logEncoder.pattern = "%-12date{YYYY-MM-dd HH:mm:ss.SSS} %-5level - %msg%n"
        logEncoder.start()

        val logFileAppender = RollingFileAppender<ILoggingEvent>();
        logFileAppender.context = loggerContext;
        logFileAppender.name = "logfile";
        logFileAppender.encoder = logEncoder as Encoder<ILoggingEvent>;
        logFileAppender.isAppend = true;
        logFileAppender.file = "${logDir}/polaris.log";

        val logFilePolicy = TimeBasedRollingPolicy<ILoggingEvent>();
        logFilePolicy.context = loggerContext;
        logFilePolicy.setParent(logFileAppender);
        logFilePolicy.fileNamePattern = "${logDir}/archived/logfile-%d{yyyy-MM-dd}.log.zip";
        logFilePolicy.maxHistory = 7;
        logFilePolicy.start();

        logFileAppender.setRollingPolicy(logFilePolicy);
        logFileAppender.start();

        root.addAppender(logFileAppender)

    }

<<<<<<< HEAD
	fun logInit(){
		// TODO delete (for log4j)
//		Configurator.setRootLevel(if(Util.isDevMode ) Level.DEBUG else Level.INFO)
//		Loggers.resetLoggerFactory()

		val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
		logger.level = Level.INFO
//		logger.isAdditive = true
		(logger.getAppender("console") as ConsoleAppender).let { app ->
			app.encoder = PatternLayoutEncoder().apply{
//				pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %highlight(%-5level) %logger{36} - %msg%n"
				pattern = "%d{HH:mm:ss} [%thread] %highlight(%-5level) %logger{36} - %msg%n"
				context = app.context
				start()
			}
		}

//		(LoggerFactory.getLogger("org.mongodb.driver.protocol.command") as ch.qos.logback.classic.Logger).let {
//			it.level = Level.DEBUG
//		}

//		private static Logger createLoggerFor(String string, String file) {
//			LoggerContext lc =(LoggerContext) LoggerFactory . getILoggerFactory ();
//			PatternLayoutEncoder ple = new PatternLayoutEncoder();
//
//			ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
//			ple.setContext(lc);
//			ple.start();
//			FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
//			fileAppender.setFile(file);
//			fileAppender.setEncoder(ple);
//			fileAppender.setContext(lc);
//			fileAppender.start();
//
//			Logger logger =(Logger) LoggerFactory . getLogger (string);
//			logger.addAppender(fileAppender);
//			logger.setLevel(Level.DEBUG);
//			logger.setAdditive(false); /* set to true if root should log too */
//
//			return logger;
//		}
	}


=======
>>>>>>> master
}
