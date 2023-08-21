package io.userhabit.common

//import com.maxmind.geoip2.DatabaseReader
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.connection.ClusterConnectionMode
import com.mongodb.connection.ClusterSettings
import com.mongodb.reactivestreams.client.ClientSession
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import io.userhabit.batch.*
import io.userhabit.batch.indicators.*
import io.userhabit.polaris.Protocol
import io.userhabit.polaris.service.*
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.Loggers
import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.name
import org.bson.Document as D

object MongodbUtil {
    private val log = Loggers.getLogger(this.javaClass)
    val SAAS_DB_NAME = "userhabit"

    //  private val clientAsync = MongoClientsAsync.create(clientSettings)
    private val client by lazy {

        val settings = MongoClientSettings.builder()

        val AUTHDB = "userhabit"
        val mongodbId = Config.get("mongodb.id")
        val mongodbPw = Config.get("mongodb.pw")
        val credentials = MongoCredential.createCredential(
            mongodbId, AUTHDB, mongodbPw.toCharArray()
        )
        settings.credential(credentials)

        val address = Config.get("mongodb.addresses")
        if (address.split(":")[0] == "localhost"){
            val uri = ConnectionString("mongodb://${address}")
            settings.applyConnectionString(uri)
        }else {
            val replicaSet = Config.get("mongodb.replicaSet")
            val readPreference = Config.get("mongodb.readPreference")
            val uri = ConnectionString("mongodb://${address}/?replicaSet=${replicaSet}&readPreference=${readPreference}")
            settings.applyConnectionString(uri)

            // settings.readPreference(ReadPreference.nearest())
        }
        MongoClients.create(settings.build())

    }

    private val db by lazy {
        client.getDatabase(SAAS_DB_NAME)
    }

    fun getCollection(collectionName: String): MongoCollection<D> {
        return db.getCollection(collectionName)
    }

    fun startSession(): Mono<ClientSession> {
        return Mono.from(client.startSession())
    }

    /**
     *
     * @param collectionName
     * @param query
     * @param projection
     * @param sortK
     * @param sortV : 1 or 0
     * @param skip : 0 ~
     * @param limit : 0 ~
     * @return
     */
    fun basicFind(
        collectionName: String,
        query: D,
        projection: D,
        sortK: String,
        sortV: Int,
        skip: Int,
        limit: Int
    ): Flux<D> {
        val find = getCollection(collectionName)
            .find(query)
            .projection(projection)
            .sort(D(sortK, sortV))
            .skip(skip)
            .limit(limit)
        return Flux.from(find)
    }

    // 2020.12.16 모든 Api에서 변경 완료 후 주석 처리 by cjh
//	fun putOnQuery(appIdList: List<String>, userId: String, collName: String, query: List<D>): List<D>{
//		return listOf(
//			D("\$match", D("\$expr", D("\$and", listOf(
//				D("\$eq", listOf("\$user_id", userId)),
//				if(appIdList.isNotEmpty()) D("\$in", listOf("\$_id", appIdList)) else D(),
//			)))),
//			D("\$group", D()
//				.append("_id", "\$user_id")
//				.append("ail", D("\$push", "\$_id"))
//			),
//			D("\$lookup", D()
//				.append("from", collName)
//				.append("let", D("ail", "\$ail"))
//				.append("pipeline", query)
//				.append("as", "data")
//			),
//			D("\$unwind", "\$data"),
//			D("\$replaceWith", "\$data"),
//		)
//	}

    // when the server starts, it reads the json files and upserts the collection data into mongodb
    init {
        val initColl = System.getProperty("initcoll")
        runMongoServer()
        if (initColl == "true") {
            initCollection()
        }
        // remove this,
        // @see https://github.com/userhabit/uh-issues/issues/871
        //
        // BatchUtil.start()
    } // end of init()

    private fun initCollection() {
        if (!System.getProperty("sun.java.command").contains("io.userhabit.Server")) {
            return
        }

        this._subscribeMongoCollBulkWriteStream()
        this._subscribeMongoGeoIpBulkWriteStream()
        this._subscribeMongoIndexCreateStream()

    }

    /**
     * @see https://docs.mongodb.com/manual/reference/configuration-file-settings-command-line-options-mapping/
     */
    private fun runMongoServer(): Int {
        if (!Config.isDevMode) return 0

        val path = Config.get("mongodb.home")
        val port = Config.get("mongodb.addresses").split(":")[1]

        val p = Path.of(path)
        if (!p.toFile().exists()) {
            log.error("Mongo HOME directory not found!! [${path}]")
            return 0
        }
        val dbPath = "${path}/${Config.get("mongodb.db_path")}"
        val logPath = "${path}/${Config.get("mongodb.log_path")}"
        Path.of(dbPath).toFile().mkdirs()
        Path.of(logPath).toFile().mkdirs()

        val ext = if (System.getProperty("os.name").lowercase().startsWith("windows")) ".exe" else ""

        ProcessBuilder().let {
            it.command(
                "${path}/bin/mongod${ext}",
                "--port", port,
                "--dbpath", dbPath,
                "--logpath", "${logPath}/mongod.log", "--logappend",
                //"--pidfilepath", "${path}/pid",
                "--fork",
                "--nojournal",
            )
            //it.inheritIO() //
            val process = it.start()
            val code = process.waitFor()
            if (code == 48) log.info("MongoDB restarted [code=${code}]")
            else if (code == 0) log.info("MongoDB started on port ${port} [code=${code}]")
            else log.error("MongoDB error [code=${code}]")

            return code
        }
    }

    fun getFieldList(collectionName: String): Mono<List<Map<String, String>>> {
        //db.session.aggregate([
        //	{"$limit":1},
        //	{"$project":{"array":{"$objectToArray":"$$ROOT"}}},
        //	{"$project":{"_id":0, "keys":"$array.k"}}
        //])
        // TODO limit 1 에 필드가 없을 수 있음. 전체 다 나오게 하는 방법은 없을까?
        return Mono
            .from(
                getCollection(collectionName).aggregate(
                    listOf(
                        D("\$limit", 1),
                        D("\$project", D("array", D("\$objectToArray", "\$\$ROOT"))),
                        D("\$project", D("keys", "\$array.k")),
                    )
                )
            )
            .switchIfEmpty(Mono.just(D("Field List", "Empty !"))) // Todo: Debugs
            .map map@{
//				println("Get [$collectionName] Filed List >> $it") // TODO del??

                @Suppress("UNCHECKED_CAST")
                val list = it["keys"] as List<String>?
                list?.map {
                    mapOf(it to Protocol.getFullname(it))
                }
                // if list is null
                    ?: return@map listOf<Map<String, String>>()
            }
            .doOnError {
                println("Error : ${it.message}")
            }
    }

//    @deprecated
//    private fun _setFieldList(): Mono<Unit> {
//        // defaultIfEmpty를 안하면 컬랙션이 없을 경우(배치가 실행 안됐을 경우) 전체 zip 함수 실행 안됨
//        // TODO 데이터가 없어도 배치가 실행되면 컬랙션 실행되게 해보자.
//        val emptyList = listOf(mapOf("" to ""))
//        // 처음엔 각 클래스마다 init{} 함수에 구현했는데 컬랙션 생선 전에 실행 돼서 오류 발생
//        return Mono.zip(
//                {
//                    // nothing
//                },
//                getFieldList(AppService.COLLECTION_NAME).map {
//                    AppService.FIELD_LIST = it
//                },
//                getFieldList(CohortService.COLLECTION_NAME).map { CohortService.FIELD_LIST = it },
//                Mono
//                        .zip(
//                                getFieldList(CrashList.COLLECTION_NAME_PT10M).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList)
//                        )
//                        .map {
//                            CrashService.FIELD_LIST_MAP = mapOf(
//                                    CrashList.COLLECTION_NAME_PT10M to it.t1,
//                                    IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H to it.t2
//                            )
//                        },
//                Mono
//                        .zip(
//                                getFieldList("one").defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorDeviceByView.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                        )
//                        .map {
//                            DeviceService.FIELD_LIST_MAP = mapOf(
//                                    "one" to it.t1,
//                                    IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT24H to it.t2,
//                                    IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H to it.t3,
//                                    IndicatorDeviceByView.COLLECTION_NAME_PT24H to it.t4,
//                            )
//                        },
//                Mono
//                        .zip(
//                                getFieldList(IndicatorAllByApp.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorAllByView.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                        )
//                        .map {
//                            DwellTimeService.FIELD_LIST_MAP = mapOf(
//                                    IndicatorAllByApp.COLLECTION_NAME_PT24H to it.t1,
//                                    IndicatorAllByView.COLLECTION_NAME_PT24H to it.t2,
//                            )
//                        },
//                // Todo : Add
//                Mono
//                        .zip(
//                                getFieldList(ViewList.COLLECTION_NAME_PT10M).defaultIfEmpty(emptyList),
//                                getFieldList(ViewList.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                        )
//                        .map {
//                            ViewService.FIELD_LIST_MAP = mapOf(
//                                    ViewList.COLLECTION_NAME_PT10M to it.t1,
//                                    ViewList.COLLECTION_NAME_PT24H to it.t2,
//                            )
//                        },
//                Mono
//                        .zip(
//                                getFieldList(ObjectList.COLLECTION_NAME_PT10M).defaultIfEmpty(emptyList),
//                                getFieldList(ObjectList.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorObjectByView.COLLECTION_NAME_PT10M).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorObjectByView.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorByTrackingFlow.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                        )
//                        .map {
//                            ObjectService.FIELD_LIST_MAP = mapOf(
//                                    ObjectList.COLLECTION_NAME_PT10M to it.t1,
//                                    ObjectList.COLLECTION_NAME_PT24H to it.t2,
//                                    IndicatorObjectByView.COLLECTION_NAME_PT10M to it.t3,
//                                    IndicatorObjectByView.COLLECTION_NAME_PT24H to it.t4,
//                                    IndicatorByTrackingFlow.COLLECTION_NAME_PT24H to it.t5,
//                            )
//                        },
//                Mono
//                        .zip(
//                                getFieldList(IndicatorByFlow.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorByEachFlow.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                        )
//                        .map {
//                            FlowService.FIELD_LIST_MAP = mapOf(
//                                    IndicatorByFlow.COLLECTION_NAME_PT24H to it.t1,
//                                    IndicatorByEachFlow.COLLECTION_NAME_PT24H to it.t2,
//                            )
//                        },
//                Mono
//                        .zip(
//                                getFieldList(IndicatorHeatmapByView.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorHeatmapByView.COLLECTION_NAME_PT10M).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorHeatmapByScrollView.COLLECTION_NAME_PT10M).defaultIfEmpty(emptyList),
//                                getFieldList(IndicatorHeatmapByScrollView.COLLECTION_NAME_PT24H).defaultIfEmpty(emptyList),
//                        )
//                        .map {
//                            HeatmapService.FIELD_LIST_MAP = mapOf(
//                                    IndicatorHeatmapByView.COLLECTION_NAME_PT10M to it.t1,
//                                    IndicatorHeatmapByScrollView.COLLECTION_NAME_PT10M to it.t2,
//                                    IndicatorHeatmapByView.COLLECTION_NAME_PT24H to it.t3,
//                                    IndicatorHeatmapByScrollView.COLLECTION_NAME_PT24H to it.t4
//                            )
//                        },
//                getFieldList(PlanHistoryService.COLLECTION_NAME).map { PlanHistoryService.FIELD_LIST = it },
//                getFieldList(PlanService.COLLECTION_NAME).map { PlanService.FIELD_LIST = it },
//                // TODO Add
//                getFieldList(LoginHistoryService.COLLECTION_NAME).map { LoginHistoryService.FIELD_LIST = it },
//                getFieldList(ReplayService.COLLECTION_NAME).map { ReplayService.FIELD_LIST = it },
//                getFieldList(SessionService.COLLECTION_NAME).map { SessionService.FIELD_LIST = it },
//                getFieldList(EventService.COLLECTION_NAME).map { EventService.FIELD_LIST = it },
//                //getFieldList(StorageService.COLLECTION_NAME).map { StorageService.FIELD_LIST = it },
//                getFieldList(MemberService.COLLECTION_NAME).map { MemberService.FIELD_LIST = it },
//                getFieldList(CompanyService.COLLECTION_NAME).map { CompanyService.FIELD_LIST = it },
//        )
//    }

    /**
     * @return { 49518=(RW, Rwanda), 51537=(SO, Somalia), ... }
     */
    private fun _getIsoMap(): MutableMap<String, Pair<String, String>> {
        val isoMap = mutableMapOf<String, Pair<String, String>>()
        Util.getResource("geolite_file/GeoLite2-Country-Locations-en.csv") {
            val lines = Files.readAllLines(it).drop(1)
            lines.map { line ->
                val row = line.split(",")
                isoMap[row[0]] = row[4] to row[5]
            }
        }
        return isoMap
    }

    /**
     * @return ipv6: 2a06:4144:c668::/47,2510769,2750405,,0,0
     *         ipv4: 223.255.236.0/22,1814991,1814991,,0,0
     *         + : 2a05:8600::/29,2510769,2510769,,0,0, 2a05:8640::/29,130758,130758,,0,0, 2a05:8680::/29,6251999,2017370,,0,0, 2a05:86c0::/29,614540,614540,,0,0
     */
    private fun getIpAddrFileListStream(): Mono<List<String>> {
        val ipv4 = Util.getResource("geolite_file/GeoLite2-Country-Blocks-IPv4.csv") {
            Files.readAllLines(it).drop(1)
        }
        val ipv6 = Util.getResource("geolite_file/GeoLite2-Country-Blocks-IPv6.csv") {
            Files.readAllLines(it).drop(1)
        }
        return Mono.just(ipv4 + ipv6)
    }

    private fun _getMongoBulkWriteStream(
        allLines: List<String>,
        coll: MongoCollection<D>,
        isoMap: MutableMap<String, Pair<String, String>>,
        biV4: BigInteger,
        biV6: BigInteger
    ): Mono<BulkWriteResult> {
        val updateModels = allLines.map { line ->

            val (ip_cidr, id) = (line as String).split(",")
            val (ip, cidr) = ip_cidr.split("/")
            val inet = InetAddress.getByName(ip)
            val address = inet.address

            val min = BigInteger(1, address)
            val max =
                if (inet is Inet6Address) min.or(biV6.shiftRight(cidr.toInt()))
                else min.or(biV4.shiftRight(cidr.toInt()))

            val iso = isoMap[id]

            UpdateOneModel<D>(
                D("_id", ip),
                D(
                    "\$set", D(Protocol.icoK, iso?.first).append("country", iso?.second)
                        // 16진수 문자로 하는 이유, toBigDecimal() 사용하면 내부적으로 Decimal128 형 변환이 이루어지는데 Decimal128 타입이 부호를 갖고 있는 타입이라 ipv6의 부호없는 128bit 숫자를 처리할 수 없기 때문에. 몽고 디비 갈길이 멀다...
                        // Caused by: java.lang.NumberFormatException: Conversion to Decimal128 would require inexact rounding of 3526142879863516208564147688489091072
                        .append("min", min.toString(16)).append("max", max.toString(16))
                        .append("cidr", cidr.toInt())
                        .append("max_minus_min", max.minus(min).toBigDecimal())
                ),
                UpdateOptions().upsert(true)
            )
        }
        return Mono.from(coll.bulkWrite(updateModels))

    }

    private fun _subscribeMongoCollBulkWriteStream() {
        val collFileReadStream: Flux<Pair<String, String>> =
            Flux.fromIterable(Util.getResourceList("init_collection") { file ->
                Pair(file.name, Files.readString(file))
            })

        val mongoCollBulkWriteStream = collFileReadStream.flatMap { fileItem ->
            // fileItem = (fileName, fileContents)
            val fileName = fileItem.first.split(".")[0]
            val updateModels = Util.jsonToList(fileItem.second).map { item ->
                val m = D.parse(Util.toJsonString(item))
                UpdateOneModel<D>(
                    if (m.containsKey("_id")) D("_id", m.remove("_id")) else D(),
                    D("\$set", m),
                    UpdateOptions().upsert(true)
                )
            }
            Mono.from(db.getCollection(fileName).bulkWrite(updateModels))
        }
        mongoCollBulkWriteStream.subscribe()
    }

    private fun _subscribeMongoGeoIpBulkWriteStream() {
        val geoCollection = db.getCollection(IpToIsoBatch.COLLECTION_NAME)
        val biV4 = BigInteger.valueOf(0xffff_ffffL)
        val biV6 = BigInteger(1, (1..16).map { 255.toByte() }.toByteArray())// 8bit * 16 = 128bit

        val isoMap = this._getIsoMap()
        val fileReadStream = Flux.from(geoCollection.find().first())
            .cast(Any::class.java)
            .switchIfEmpty(this.getIpAddrFileListStream())

        val mongoWriteStream = fileReadStream.flatMap { allLines ->
            if (allLines is List<*>) {
                // is empty, insert records
                this._getMongoBulkWriteStream(allLines as List<String>, geoCollection, isoMap, biV4, biV6)
            } else {
                // empty, do nothing
                Mono.empty()
            }
        }
        mongoWriteStream.subscribe()
    }

    private fun _subscribeMongoIndexCreateStream() {
        Mono
//				.from(db.getCollection(SessionService.COLLECTION_NAME).createIndex(D("st", 1).append("ai", 1).append("av", 1)))
            .from(
                db.getCollection(SessionService.COLLECTION_NAME).createIndexes(
                    listOf(
                        IndexModel(D("st", 1).append("ai", 1).append("av", 1)),
                        IndexModel(D("i", 1))
                    )
                )
            )
            .concatWith(
                db.getCollection(MemberService.COLLECTION_NAME).createIndex(D("email", 1), IndexOptions().unique(true))
            )
            .concatWith(db.getCollection(AppService.COLLECTION_NAME).createIndex(D("user_id", 1)))
            .concatWith(db.getCollection(EventService.COLLECTION_NAME).createIndex(D("_id.si", 1)))
            .concatWith(
                db.getCollection(IndicatorAllByView.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorAllByView.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorEventByView.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("t", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("t", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorEventByView.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1).append("t", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorObjectByView.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("oi", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("oi", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorObjectByView.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1).append("oi", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(ViewList.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(ViewList.COLLECTION_NAME_PT24H).createIndex(
                    D("ai", 1)
                        .append("av", 1).append("vhi", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(ObjectList.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("oi", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("oi", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(ObjectList.COLLECTION_NAME_PT24H).createIndex(
                    D("ai", 1)
                        .append("av", 1).append("vhi", 1).append("oi", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(CrashList.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("stz", -1).append("ai", 1).append("av", 1).append("ci", 1).append("si", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(
                            D("stz", -1).append("ai", 1).append("av", 1).append("dn", 1).append("dov", 1)
                                .append("ci", 1)
                        ),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorDeviceByView.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("di", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("di", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorDeviceByView.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByOs.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("dov", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("dov", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByOs.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("dov", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByCountry.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("dl", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("dl", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByCountry.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("dl", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByDevicename.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("dn", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("dn", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByDevicename.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("dn", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByResolution.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("dw", 1).append("dh", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("dw", 1).append("dh", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByResolution.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("dw", 1).append("dh", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorByFlow.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("flow", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("flow", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorByFlow.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("flow", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorByEachFlow.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("avhi", 1)
                                .append("bvhi", 1), IndexOptions().unique(true)
                        ),
                        IndexModel(
                            D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("avhi", 1)
                                .append("bvhi", 1)
                        ),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorByEachFlow.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1).append("avhi", 1).append("bvhi", 1),
                    IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorByTrackingFlow.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("avhi", 1)
                                .append("bvhi", 1), IndexOptions().unique(true)
                        ),
                        IndexModel(
                            D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("avhi", 1)
                                .append("bvhi", 1)
                        ),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorByTrackingFlow.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1).append("avhi", 1).append("bvhi", 1),
                    IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorAllByApp.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(D("st", -1).append("ai", 1).append("av", 1), IndexOptions().unique(true)),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorAllByApp.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorDeviceByApp.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("di", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("di", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorDeviceByApp.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("di", 1).append("dov", 1).append("dn", 1)
                                .append("ci", 1), IndexOptions().unique(true)
                        ),
                        IndexModel(
                            D("stzd", -1).append("ai", 1).append("av", 1).append("di", 1).append("dov", 1)
                                .append("dn", 1).append("ci", 1)
                        ),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionDeviceByAppIsCrash.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("dov", 1).append("dn", 1).append("ci", 1),
                    IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByOsDevicename.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("dov", 1).append("dn", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("dov", 1).append("dn", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorSessionByOsDevicename.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("dov", 1).append("dn", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("di", 1).append("dov", 1)
                                .append("dn", 1), IndexOptions().unique(true)
                        ),
                        IndexModel(
                            D("stzd", -1).append("ai", 1).append("av", 1).append("di", 1).append("dov", 1)
                                .append("dn", 1)
                        ),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorDeviceByOsDevicename.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("dov", 1).append("dn", 1).append("di", 1),
                    IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorDeviceByViewIsCrash.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("di", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("di", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorDeviceByViewIsCrash.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorEventByApp.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("t", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(D("stzd", -1).append("ai", 1).append("av", 1).append("t", 1)),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorEventByApp.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("t", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorHeatmapByView.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("stz", -1).append("st", -1).append("ai", 1).append("av", 1).append("vhi", 1)
                                .append("t", 1).append("vo", 1).append("hx", 1).append("hy", 1).append("hex", 1)
                                .append("hey", 1).append("fevent", 1).append("levent", 1), IndexOptions().unique(true)
                        )
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorReachRateByScrollView.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("svi", 1)
                                .append("spx", 1).append("spy", 1), IndexOptions().unique(true)
                        ),
                        IndexModel(
                            D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("svi", 1)
                                .append("spx", 1).append("spy", 1)
                        ),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorReachRateByScrollView.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1).append("svi", 1).append("spx", 1)
                        .append("spy", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(IndicatorHeatmapByScrollView.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("st", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("svi", 1).append("t", 1)
                                .append("spx", 1).append("spy", 1), IndexOptions().unique(true)
                        ),
                        IndexModel(
                            D("stzd", -1).append("ai", 1).append("av", 1).append("vhi", 1).append("svi", 1)
                                .append("t", 1).append("spx", 1).append("spy", 1)
                        ),
                        IndexModel(D("bft", -1))
                    )
                )
            )
            .concatWith(
                db.getCollection(IndicatorHeatmapByScrollView.COLLECTION_NAME_PT24H).createIndex(
                    D("stz", 1)
                        .append("ai", 1).append("av", 1).append("vhi", 1).append("svi", 1).append("t", 1)
                        .append("spx", 1).append("spy", 1), IndexOptions().unique(true)
                )
            )
            .concatWith(
                db.getCollection(CohortPreview.COLLECTION_NAME_PT10M).createIndexes(
                    listOf(
                        IndexModel(
                            D("stz", 1).append("ai", 1).append("av", 1).append("si", 1),
                            IndexOptions().unique(true)
                        ),
                        IndexModel(
                            D("stz", 1).append("ai", 1).append("av", 1).append("si", 1).append("di", 1).append("dn", 1)
                                .append("dt", 1).append("dl", 1).append("dz", 1).append("first_view", 1)
                                .append("last_view", 1).append("total_view_count", 1).append("unique_view_count", 1)
                        )
                    )
                )
            )
            .concatWith(
                db.getCollection(IpToIsoBatch.COLLECTION_NAME).createIndex(
                    D("min", 1).append("max", 1),
                    IndexOptions().unique(true),
                )
            )
            .subscribe()
    }
}

