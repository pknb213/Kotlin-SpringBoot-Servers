package io.userhabit.polaris.service

import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.userhabit.common.MongodbUtil
import io.userhabit.common.SafeRequest
import io.userhabit.common.Status
import io.userhabit.polaris.model.EventColl
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import io.userhabit.polaris.EventType as ET
import io.userhabit.polaris.Protocol as P
import org.bson.Document as D

/**
 * @author sbnoh
 */
object SessionEventService {
    private val log = Loggers.getLogger(this.javaClass)

    fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
        val sessionList = req.getBodyJsonToList()
        return AppService.getAppIdMap(sessionList.map { it[P.ak] as String })
            .flatMap { appIdMap ->  // { appkey: _id(app) }
                Mono.zip(
                    // Event zip1
                    Flux
                        .fromIterable(sessionList)
                        .flatMap { session ->
                            session as MutableMap
                            val eventList = session[P.e] as List<MutableMap<String, Any>>
                            // @see ViewPeasant.getVhi()
                            val ofViewHashId = session[P.vi].hashCode() // if session[P.vi] is null, hashCode() returns '0'

                            var viewUniqueTime = if(eventList.isNotEmpty()) {
                                eventList.first()[P.ts]
                            } else{
                                0L
                            }
                            // 임시 처리: P.i는 벤치마크 시에만 들어가는 시간 필드
                            if (session[P.i] != null) session[P.i] = Date(session[P.i] as Long)
                            Flux
                                .fromIterable(eventList)
                                .map { event ->
                                    var viewHashId = 0
                                    var crashId = 0
                                    val t = (event[P.t] as Number).toInt()
                                    // @see ViewPeasant.getVhi()
                                    if (event[P.vi] != null) viewHashId = event[P.vi].hashCode()
                                    if (t == ET.VIEW_START || t == ET.APP_START || t == ET.APP_END) viewUniqueTime = event[P.ts]
                                    // P.i 검사는 벤치마크에서 전송시 배치처리에 영향을 안주기 위해. SDK 에는 i가 없으므로 LocalDateTime이 입력된다.
                                    if (t == ET.APP_END && session[P.i] == null) session[P.i] = LocalDateTime.now(ZoneOffset.UTC)
                                    if (t == ET.CRASH) crashId = "${event[P.ct]}${event[P.cm]}".hashCode()

                                    if (event.containsKey(P.t)) event[P.t] = (event[P.t] as Number).toInt()
                                    if (event.containsKey(P.vw)) event[P.vw] = (event[P.vw] as Number).toInt()
                                    if (event.containsKey(P.vh)) event[P.vh] = (event[P.vh] as Number).toInt()
                                    if (event.containsKey(P.gi)) event[P.gi] = (event[P.gi] as Number).toInt()
                                    if (event.containsKey(P.gx)) event[P.gx] = (event[P.gx] as Number).toInt()
                                    if (event.containsKey(P.gy)) event[P.gy] = (event[P.gy] as Number).toInt()
                                    if (event.containsKey(P.gex)) event[P.gex] = (event[P.gex] as Number).toInt()
                                    if (event.containsKey(P.gey)) event[P.gey] = (event[P.gey] as Number).toInt()
                                    if (event.containsKey(P.gv)) event[P.gv] = (event[P.gv] as Number).toInt()
                                    if (event.containsKey(P.spx)) event[P.spx] = (event[P.spx] as Number).toInt()
                                    if (event.containsKey(P.spy)) event[P.spy] = (event[P.spy] as Number).toInt()
                                    if (event.containsKey(P.cb)) event[P.cb] = (event[P.cb] as Number).toInt()
                                    if (event.containsKey(P.cn)) event[P.cn] = (event[P.cn] as Number).toInt()
                                    if (event.containsKey(P.cp)) event[P.cp] = (event[P.cp] as Number).toInt()
//									if(event.containsKey(P.mn)) event[P.mn] = (event[P.mn] as Number).toInt()
                                    if (event.containsKey(P.kf)) event[P.kf] = (event[P.kf] as Number).toInt()
//									if(event.containsKey(P.vo)) event[P.vo] = (event[P.vo] as Number).toInt() TODO deprecated

                                    val setValue = D(event)
                                        .append(P.vhi, viewHashId)
                                        .append(P.uts, viewUniqueTime)
                                        .append(P.ci, crashId)
                                        .append(EventColl.ofvhi, ofViewHashId)  // insert the ofvhi value even the value is 0

                                    UpdateOneModel<D>(
                                        D("_id", D(P.si, session[P.si]).append(P.ts, event.remove(P.ts))),
                                        D("\$set", setValue),
                                        UpdateOptions().upsert(true))
                                }
                        }
                        .collectList()
                        .flatMap {
                            if (it.isNotEmpty()) Mono.from(MongodbUtil.getCollection(EventService.COLLECTION_NAME).bulkWrite(it))
                                .map {
                                    mapOf(
                                        "insert_count" to it.upserts.size,
                                        "update_count" to it.matchedCount
                                    )
                                }
                            else Mono.just(mapOf<String, String>())
                        },
                    // Session zip2
                    Flux
                        .fromIterable(sessionList)
                        .filter { session ->
                            // 2022-03-04
                            // 'se' field is used to detemine if the session is initial session or not
                            // This decision is made after the discussion.
                            // ref: https://github.com/userhabit/uh-issues/issues/768
                            (session[P.se] != null)
                        }
                        .map { session ->
                            // TODO: modify the value of parameter is not a good idea
                            // it's better to use new variable instead of 'session parameter'
                            session as MutableMap

                            session.remove(P.e) // not to set events in the 'session' collection

                            // server time
                            session[P.i] = LocalDateTime.now(ZoneOffset.UTC)
                            // session.st value from sdk comes in nanoseconds
                            if (session[P.st] is Long) session[P.st] = Date(session[P.st] as Long)
                            session[P.ai] = ObjectId(appIdMap[session[P.ak]]!!) // Todo: Str to ObjectId
                            val inet = req.getInetAddress()
                            session[P.ip] = inet.hostAddress
                            // 몽고는 왜 BigInteger(unsigned 128bit decimal)는 지원하지 않는가...
                            session[P.ipi] = BigInteger(1, inet.address).toString(16)
                            //TODO delete
//							session[P.ipi] = "72ce45b3"
//							if(session.containsKey(P.dl)) session[P.ico] = (session[P.dl] as String).split("_").get(1) // Todo: ICO Add 2.21, GEO Batch에서 추가 해 줌
                            if (session.containsKey(P.ab)) session[P.ab] = (session[P.ab] as Number).toInt()
                            if (session.containsKey(P.dd)) session[P.dd] = (session[P.dd] as Number).toInt()
                            if (session.containsKey(P.dh)) session[P.dh] = (session[P.dh] as Number).toInt()
                            if (session.containsKey(P.`do`)) session[P.`do`] = (session[P.`do`] as Number).toInt()
                            if (session.containsKey(P.dw)) session[P.dw] = (session[P.dw] as Number).toInt()
                            if (session.containsKey(P.se)) session[P.se] = (session[P.se] as Number).toInt()
                            if (session.containsKey(P.sn)) session[P.sn] = (session[P.sn] as Number).toInt()
                            if (session.containsKey(P.usv)) session[P.usv] = (session[P.usv] as Number).toInt()

                            UpdateOneModel<D>(
                                D("_id", session.remove(P.si)),
                                D("\$set", D(session)),
                                UpdateOptions().upsert(true))
                        }
                        .collectList()
                        .flatMap {
                            if (it.isNotEmpty()) Mono.from(MongodbUtil.getCollection(SessionService.COLLECTION_NAME).bulkWrite(it))
                                .map {
                                    mapOf(
                                        "insert_count" to it.upserts.size,
                                        "update_count" to it.matchedCount
                                    )
                                }
                            else Mono.just(mapOf<String, String>())
                        },
                    // ak data validation zip3
                    Mono
                        .just(sessionList.map { mapOf(it[P.ak] as String to appIdMap.containsKey(it[P.ak])) })
                )
            }
            .map {
                Status.status200Ok(mapOf(
                    P.edrK to it.t1,
                    P.sdrK to it.t2,
                    P.akvK to it.t3,
                ))
            }
    }

}
