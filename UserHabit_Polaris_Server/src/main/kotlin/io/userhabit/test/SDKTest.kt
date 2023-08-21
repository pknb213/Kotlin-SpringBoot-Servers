package io.userhabit.test

import io.userhabit.common.Util
import io.userhabit.polaris.EventType as ET
import reactor.netty.resources.ConnectionProvider
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import io.userhabit.polaris.Protocol as P

object SDKTest {
    private val TEST_MODE_SEND_EVENT = "event"

    @JvmStatic
    fun main(args: Array<String>) {
        val testMode = System.getProperty("mode", "event").toString()
        when(testMode){
            TEST_MODE_SEND_EVENT-> sendEvent()
            else->println("wrong select mode ${testMode}")
        }
    }

    fun sendEvent(){
        val eventCount = System.getProperty("count", "100").toString()
        val uri = ClassLoader.getSystemResource("init_collection/app.json").toURI()

        val appInformationSet = Util.jsonToList(Files.readString(Paths.get(uri).toFile().toPath())).toSet()
        val eventTypeSet = setOf<Any>(
            ET.NOACT_TAP,
            ET.NOACT_DOUBLE_TAP,
            ET.NOACT_LONG_TAP,
            ET.NOACT_SWIPE,
            ET.REACT_TAP,
            ET.REACT_DOUBLE_TAP,
            ET.REACT_LONG_TAP,
            ET.REACT_SWIPE,
            ET.KEYBOARD_ENABLE,
            ET.KEYBOARD_DISABLE,
            ET.SCROLL_CHANGE,
            ET.CRASH,
            ET.VIEW_START,
            ET.VIEW_END,

            ET.APP_FOREGROUND,
            ET.APP_BACKGROUND
        )

        println("file? ${appInformationSet.random()}")
        println("file? ${appInformationSet.random()[P.akK]}")

        val sessionInformationMap = mapOf<String, Any>(
                P.ak to appInformationSet.random()[P.akK] as String,
                P.ab to rand(10,20),
                P.av to "${rand(1,10)}.${rand(0,10)}.${rand(0,10)}",
                P.usv to rand(1, 100000),
                P.dc to "dcdcdcd",
                P.db to "dbdbdbdbdb",
                P.di to "${rand(1,30).hashCode()}",
                P.dm to "dmdmdmdm",
                P.dn to "dndndndndn",
                P.dw to 1080,
                P.dh to 2080,
                P.dd to 480,
                P.dov to rand(5,15),
                P.dl to "ko_kr",
                P.dz to "Asia/Seoul",
                P.`do` to 1,
                P.si to rand(1,1000000000),
                P.st to Date().time,
                P.se to rand(1,20000),
                P.sn to 7
        )


        val eventList = mutableListOf<Map<String, Any>>(

        )
        for(i in 1..rand(1,1000)){

        }



//
//            "e" : [
//            {"t" : 4096, "ts" : 0, "vi" : "###SESSION_START###"},
//            {"t" : 4353, "ts" : 1, "vi" : "ActionActivity"},
//            {"t" : 8192, "ts" : 2, "gx" : 1, "gy" : 1,"gsx":20,"gsy":100,"go":"objobj","gd":"obj dedede","svi":"scrollview id ididi"},
//            {"t" : 4354, "ts" : 3},
//            {"t" : 4353, "ts" : 4, "vi" : "SmartActivity"},
//            {"t" : 8192, "ts" : 5, "gx" : 1, "gy" : 1},
//            {"t" : 4354, "ts" : 6},
//            {"t" : 4354, "ts" : 7},
//            {"t" : 4097, "ts" : 70, "vi" : "###SESSION_END###"}
//            ]
//        }
//        ]



        val cp = ConnectionProvider.builder("benchmark")
                .maxConnections(1)
                .pendingAcquireMaxCount(100000) // TODO 줄여보자
                .build()

//		val sessionUrl = "${Config.get("http.host")}:${Config.get("http.port")}/v3/session"
//        val httpClient = HttpClient.create(cp).compress(true)
//        val eventUrl = "${Config.get("http.host")}:${Config.get("http.port")}/v3/sdk/event"
//
//        httpClient
//                .post()
//                .uri(eventUrl)
//                .send(ByteBufFlux.fromString(Mono.just(Util.toJsonString(listOf(session)))))
//                .responseContent()
//                .aggregate()
//                .asString()


    }

    fun rand(from: Int, to: Int) : Int {
        return Random().nextInt(to - from) + from
    }

    fun makeRandomEvent(eventTyep:Int):Map<String, Any>{
        val returnMap = mutableMapOf<String, Any>()

        when(eventTyep){
            ET.NOACT_TAP->{
                returnMap
            }
            ET.NOACT_DOUBLE_TAP,
            ET.NOACT_LONG_TAP,
            ET.NOACT_SWIPE,
            ET.REACT_TAP,
            ET.REACT_DOUBLE_TAP,
            ET.REACT_LONG_TAP,
            ET.REACT_SWIPE,
            ET.KEYBOARD_ENABLE,
            ET.KEYBOARD_DISABLE,
            ET.SCROLL_CHANGE,
            ET.CRASH,
            ET.VIEW_START,
            ET.VIEW_END,
            ET.APP_START,
            ET.APP_END,

            ET.APP_FOREGROUND,
            ET.APP_BACKGROUND->{
                println("event")
            }
        }


        return returnMap
    }
}
