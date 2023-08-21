package io.userhabit.polaris.service

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.reactivestreams.client.MongoCollection
import io.userhabit.batch.IpToIsoBatch
import io.userhabit.common.*
import io.userhabit.polaris.Protocol
import io.userhabit.polaris.service.geoip.GeoIpMapper
import reactor.core.publisher.Mono
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.file.Files
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import org.bson.Document as D


object GeoService {

    val geoCollection = MongodbUtil.getCollection(IpToIsoBatch.COLLECTION_NAME)
    val biV4 = BigInteger.valueOf(0xffff_ffffL)
    val biV6 = BigInteger(1, (1..16).map { 255.toByte() }.toByteArray())// 8bit * 16 = 128bit

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
     * @author yj
     * @sample GET {{localhost}}/v3/geo
     * @return data = [{
     *     "Matched Cnt": 435340,
     *     "Inserted Cnt": 0,
     *     "Modified Cnt": 0
     *  }]
     * @exception 403 Forbidden
     * @see File Line 43만 건 기준 40초 가량 걸림
     */
    fun get(req: SafeRequest): Mono<Map<String, Any>> {
        val isoMap = this._getIsoMap()
        val fileReadStream = getIpAddrFileListStream()

        return fileReadStream.flatMap { allLines ->
            if (allLines is List<*>) {
                this._getMongoBulkWriteStream(allLines as List<String>, geoCollection, isoMap, biV4, biV6)
            } else {
                // error
                Mono.error(PolarisException.status403Forbidden("Bad File: $allLines(${allLines.javaClass})"))
            }
        }.map { Status.status200Ok(_bulkWriteResult(it)) }
    }

    fun updateSessionCountryCode(req: SafeRequest): Mono<Map<String, Any>> {

        val fromDate = req.getQueryOrDefault("from_date", "")
        val toDate = req.getQueryOrDefault("to_date", "")

        val validator = Validator()
            .new(fromDate, "from_date").required().date()
            .new(toDate, "to_date").required().date()

        if (validator.isNotValid()) throw PolarisException.status400BadRequest(validator.toExceptionList())

        val isoCodePreloaded = "KR"
        val geoIpMapper = GeoIpMapper()
        val updateResStream = geoIpMapper.updateCountryCodeOfSessionListStream(isoCodePreloaded, fromDate, toDate)
        
        return updateResStream.flatMap{
            val f = DateTimeFormatter.ISO_DATE_TIME;
            val zdtFrom = ZonedDateTime.parse(fromDate, f);
            val zdtTo = ZonedDateTime.parse(toDate, f);
            IpToIsoBatch.tenMinutes(zdtFrom, zdtTo)
        }.map{
            Status.status200Ok(it)
        }
    }


    private fun _bulkWriteResult(
        res: BulkWriteResult
    ): List<Map<String, Int>> {
        return listOf(
            mapOf(
                "Matched Cnt" to res.matchedCount,
                "Inserted Cnt" to res.insertedCount,
                "Modified Cnt" to res.modifiedCount
            )
        )
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

    private fun getIpAddrFileListStream(): Mono<List<String>> {
        val ipv4 = Util.getResource("geolite_file/GeoLite2-Country-Blocks-IPv4.csv") {
            Files.readAllLines(it).drop(1)
        }
        val ipv6 = Util.getResource("geolite_file/GeoLite2-Country-Blocks-IPv6.csv") {
            Files.readAllLines(it).drop(1)
        }
        return Mono.just(ipv4 + ipv6)
    }

    fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
        // Todo: 이렇게 안함. 패스
        val bodyList = Util.jsonToList(req.getBodyOrDefault("meta", "[]"))
        println(bodyList)
        println(req.getFile("field-name").get("body"))
        val actionList = (req.getFile("field-name")["body"] as ByteBuffer).let {
            println("it: $it")
            Util.toInputStream(it).use { bais ->
                println("bais: $bais")
                InputStreamReader(bais).readLines().map {
                    println("Read: ${Util.toPretty(it)}")
                }
            }
        }
        println(actionList)

        return Mono.just(mapOf("1" to 1))
    }
}
