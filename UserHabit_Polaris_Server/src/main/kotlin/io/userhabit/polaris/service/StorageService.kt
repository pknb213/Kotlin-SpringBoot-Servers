package io.userhabit.polaris.service

import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpResponseStatus
import io.userhabit.common.*
import io.userhabit.common.utils.FileNamePeasant
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import org.bson.Document as D
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.netty.http.client.HttpClient
import reactor.util.Loggers
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.lang.UnsupportedOperationException
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import io.userhabit.polaris.Protocol as P

/**
 * @author sbnoh
 */
object StorageService {
	private val log = Loggers.getLogger(this.javaClass)
	const val COLLECTION_NAME = "storage"
	const val TYPE_LOCAL = "file:"
	const val TYPE_FTP = "ftp:"
	const val TYPE_HTTP = "http:"
	const val TYPE_S3 = "s3:"
	const val TYPE_GOOGLE_STORAGE = "gcp:"
	val PATH = Config.get("storage.path")
		.let { if(it[0] == '/') it else "${System.getProperty("user.home")}/${it}" }
	const val URI = "/v3/storage/file"
	// @deprecated
	// lateinit var FIELD_LIST: List<Map<String, String>>

	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val ids = req.getParamOrDefault("ids","")

		val appKey = req.getQueryOrDefault(P.ak,"")
		val appId = req.getQueryOrDefault(P.ai,"")
		val appVersion = req.getQueryOrDefault(P.av,"")
		val viewId = req.getQueryOrDefault(P.vi,"")
		val objectId = req.getQueryOrDefault(P.oi,"")
		val scrollViewId = req.getQueryOrDefault(P.svi,"")

		val searchField = req.getQueryOrDefault("search_field", "")
		val searchValue = req.getQueryOrDefault("search_value", "")
		val sortField = req.getQueryOrDefault("sort_field", "cd")
		val sortValue = req.getQueryOrDefault("sort_value", -1)
		val skip = req.getQueryOrDefault("skip",0)
		val limit = req.getQueryOrDefault("limit", 20)
		val fieldList = req.splitQueryOrDefault("field_list", "${P.ai},${P.av},${P.fhi},${P.sty},${P.cd}")

		val vali = Validator()
			.new("${appKey}${appId}", "${P.ak} or ${P.ai}").required()
			.new(appVersion, P.av).required()
//      .new(viewId + objectId + scrollViewId, "${P.vi} or ${P.oi} or ${P.svi}").required()
			.new(limit, "limit").max(100)
		if(vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return if(appKey.isNotEmpty()){
			AppService.getAppIdMap(listOf(appKey)).map { it[appKey].toString() }
		}else{ // appId로 검색되게
			Mono.just(appId)
		}
			.flatMapMany{ appId ->
				Flux
					.from(MongodbUtil.getCollection(COLLECTION_NAME)
						.aggregate(listOf(
							D("\$match", D()
								.append(P.ai, appId)
								.append(P.av, appVersion)
								.let{
									if(viewId.isNotEmpty()) it.append(P.vi, viewId)
									if(objectId.isNotEmpty()) it.append(P.oi, objectId)
									if(scrollViewId.isNotEmpty()) it.append(P.svi, scrollViewId)
									if(searchField.isNotEmpty() && searchValue.isNotEmpty()) it.append(searchField, searchValue)
									it
								}
							),
							D("\$sort", D(sortField, sortValue)),
							D("\$skip", skip),
							D("\$limit", limit),
							D("\$project", fieldList.fold(D()){ acc, it -> acc.append(it, 1)}
								.let {
									it.append("_id", D("\$toString", "\$_id"))
									if(it.containsKey("created_date")) it.append("created_date", D("\$dateToString", D("date", "\$created_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
									if(it.containsKey("updated_date")) it.append("updated_date", D("\$dateToString", D("date", "\$updated_date").append("format", "%Y-%m-%dT%H:%M:%SZ")))
									it
								}),
						))
					)
			}
			.map { doc ->
				doc.remove(P.sty).let {
					doc["path"] = "${if(it == TYPE_LOCAL) URI else it}/${doc[P.ai]}/${doc[P.av]}/${doc[P.fhi]}"
				}
				doc
			}
			.collectList()
			.map {
				Status.status200Ok(it)
			}

	} //  end of get()

	private val storageType = Config.get("storage.type")
	private val writeStorageFunc = if(storageType == TYPE_LOCAL) {
		::writeLocalStorage
	} else if(storageType == TYPE_S3) {
		::writeS3
	} else if(storageType == TYPE_HTTP) {
		::writeHttp
	} else {
		::writeLocalStorage
	}

	fun postAndPut(req: SafeRequest): Mono<Map<String, Any>> {
		val bodyList = Util.jsonToList(req.getBodyOrDefault("meta", "[]"))

		val vali = Validator()
		if(bodyList.isEmpty()) vali.new("", "meta").required()

		bodyList.forEachIndexed { i, it ->
			val fileId = (it[P.oi] ?: it[P.svi] ?: it[P.vi] ?: "").toString()
			vali.new(it.getOrDefault(P.ak, it.getOrDefault(P.ai, "")).toString(), "${P.ak} or ${P.ai}").required()
			vali.new(it, P.av).required()
			vali.new(fileId, "${P.oi} or ${P.svi} or ${P.vi} ").required()
			vali.new(it.getOrDefault("top", "false").toString(), "top").required(listOf("true", "false"))
			if (req.method() == "POST" && fileId.isNotEmpty()) {
				vali.new(if (req.getFile("file_${i}").isEmpty()) "" else "exists", "file : file_${i}").required()
			}
		}
		if (vali.isNotValid())
			throw PolarisException.status400BadRequest(vali.toExceptionList())

		return AppService.getAppIdMap(bodyList.map { it[P.ak] as String })
			.filter{ appIdMap ->
				appIdMap.isNotEmpty()
			}
			.flatMap{ appIdMap ->
				val modelList = bodyList.mapIndexed { i, bodyMap ->
					val appId = bodyMap[P.ai] ?: appIdMap[bodyMap[P.ak]]
					val fileHashId = FileNamePeasant.getFileHashId(bodyMap[P.oi] as String?, bodyMap[P.svi] as String?, bodyMap[P.vi] as String?)
					val fileMap = req.getFile("file_${i}")
					val file = fileMap["body"] as ByteBuffer
					val fileSize = file.remaining()

					val imageDocument = D()
						.append(P.vi, bodyMap[P.vi])
//            .append(P.vhi , bodyMap[P.vi]?.toString().hashCode())
						.append(P.oi , bodyMap[P.oi])
//            .append(P.ohi , bodyMap[P.oi]?.toString().hashCode())
						.append(P.svi , bodyMap[P.svi])
//            .append(P.svhi , bodyMap[P.svi]?.toString().hashCode())
//						.append(P.svn , bodyMap[P.svn]) // file index TODO 사용됨?
//						.append(P.nin , bodyMap[P.nin]) // TODO 사용됨?
						.append("top", bodyMap["top"] ?: false)
						.append(P.ud, Instant.now())

					if(req.method() == "POST"){
						imageDocument.append(P.cd, Instant.now())
					}

					if(fileSize > 0){ // 파일이 있으면
						writeStorageFunc(file , "${PATH}/${appId}/${bodyMap[P.av]}", fileHashId )

						imageDocument.append(P.fs, fileSize)
							.append(P.sty, Config.get("storage.type"))
							.append(P.cty, fileMap["type"])
							.append(P.fn, fileMap["name"])
					}

					UpdateOneModel<D>( D()
						.append(P.ai , ObjectId(appId as String))
						.append(P.av , bodyMap[P.av])
						.append(P.fhi , fileHashId),
						D("\$set", imageDocument),
						UpdateOptions().upsert(true)
					)
				}

				Mono.from(MongodbUtil.getCollection(COLLECTION_NAME).bulkWrite(modelList))
					.map{ bulkResult ->
						val result = mapOf(
							P.dbrK to bulkResult,
							P.akvK to
								bodyList.map {
									mapOf( it[P.ak] to appIdMap.containsKey(it[P.ak]) )
								}
						)
						Status.status200Ok(result)
					}
			}
			.subscribeOn(Schedulers.boundedElastic()) // TODO 할까 말까...
	} // end of postAndPut()

	/**
	 * ~/polaris/{{imageKindPath}}/{{appId}}/{{appVersion}}/{{imageId}}
	 * ex)~/polaris/image/appid01234/1.2.3/282817373
	 */
	private fun writeLocalStorage(fileBuffer: ByteBuffer, path: String, fileName: String): Unit{
		val savePath = File(path)
		if (!savePath.exists()){
			if(!savePath.mkdirs()){
				log.error("이미지 저장 디렉토리 생성 실패")
			}
		}

		return FileChannel.open(
			Paths.get("${savePath}/${fileName}"),
			StandardOpenOption.CREATE,
			StandardOpenOption.WRITE
		).use {
			it.write(fileBuffer)
		}
	}

	private fun writeFtp(fileBuffer: ByteBuffer, path: String, fileName: String) {
		throw UnsupportedOperationException()
		// TODO
	}

	private fun writeHttp(fileBuffer: ByteBuffer, path: String, fileName: String) {
		throw UnsupportedOperationException()
		// TODO
	}

	private val httpClient by lazy(LazyThreadSafetyMode.NONE){
		HttpClient.create().compress(true)
	}
	private val s3Region = Config.get("s3.region")
	private val s3Bucket = Config.get("s3.bucket")
	private val s3AccessKey = Config.get("s3.access_key")
	private val s3SecretKey = Config.get("s3.secret_key")
	private val df8601 = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
	private val dfYearMonthDay = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
	private val terminator = "aws4_request"
	private val method = "PUT"
	private val scheme = "AWS4"
	private val algorithm = "HMAC-SHA256"
	private val service = "s3"

	/**
	 *
	 * AWS S3 규약 / 이런 미친 블럭체인이야 뭐야 욕나와...
	 * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html
	 * @see https://docs.aws.amazon.com/ko_kr/AmazonS3/latest/API/sig-v4-authenticating-requests.html
	 */
	fun writeS3(fileBuffer: ByteBuffer, path: String, fileName: String) {

		val path = "/${s3Bucket}${path}/${fileName}"
//		https://groups.google.com/g/netty-ko/c/YpqNXLVAtQU
		val objectBytes = if(fileBuffer.hasArray()) fileBuffer.array() else ByteArray(fileBuffer.remaining()).let { fileBuffer.get(it); it }
		httpClient
			.headers { headers ->
				val now = Instant.now()
				val now8601 = df8601.format(now)
//				val now8601 = "20211228T062727Z"
				val nowYearMonthDay = dfYearMonthDay.format(now)
//				val nowYearMonthDay = "20211228"
				val contentHashString = Util.toHex(Util.sha256(objectBytes))

				headers.set("x-amz-storage-class", "REDUCED_REDUNDANCY")
				headers.set("x-amz-content-sha256", contentHashString)
				headers.set("x-amz-date", now8601)
				headers.set("content-length", "${objectBytes.size}")
				headers.set("Host", "s3-${s3Region}.amazonaws.com")
				val headerList = headers.entries()

				val queryParameters = mutableMapOf<String, String>().keys.map {
					"${URLEncoder.encode(it, "UTF-8")}=${URLEncoder.encode(headers[it], "UTF-8")}"
				}.sorted().joinToString("&")
				val canonicalizedHeaders = headerList.map {
					"${it.key.lowercase().replace(Regex("\\s+"), " ")}:${it.value.replace(Regex("\\s+"), " ")}"
				}.sorted().joinToString("\n") + "\n"
				val canonicalizedHeaderNames = headerList.map { it.key.lowercase() }.sorted().joinToString(";")

				// canonicalize the various components of the request
				val canonicalRequest = "${method}\n" +
					"${path}\n" +
					"${queryParameters }\n" +
					"${canonicalizedHeaders }\n" +
					"${canonicalizedHeaderNames }\n" +
					contentHashString
				log.debug("\n--------- Canonical request --------\n${canonicalRequest}\n------------------------------------")

				val scope =  "${nowYearMonthDay }/${s3Region }/${service}/${terminator}"
				val stringToSign = "${scheme }-${algorithm }\n${now8601 }\n${scope }\n${Util.toHex(Util.sha256(canonicalRequest.toByteArray()))}"
				log.debug("\n--------- String to sign --------\n${stringToSign}\n------------------------------------")

				// compute the signing key
				val signature = Util.sign(stringToSign,
					Util.sign(terminator,
						Util.sign(service,
							Util.sign(s3Region,
								Util.sign(nowYearMonthDay,
									(scheme + s3SecretKey).toByteArray())))))

				val authorizationHeader = "${scheme }-${algorithm } " +
					"Credential=${s3AccessKey }/${scope}, " +
					"SignedHeaders=${canonicalizedHeaderNames}, " +
					"Signature=${Util.toHex(signature)}"
				log.debug("\n--------- Authorization headeer --------\n${authorizationHeader}\n------------------------------------")

				headers.set("Authorization", authorizationHeader)
			}
			.put()
			.uri("https://s3-${s3Region}.amazonaws.com${path}")
			.send(Mono.just(Unpooled.wrappedBuffer(objectBytes)))
			.responseSingle{ resp, bbm ->
				bbm
					.asString()
					.defaultIfEmpty("")
					.map { xml ->
						if(resp.status().code() != HttpResponseStatus.OK.code() ){
							val transformer = TransformerFactory.newInstance().newTransformer()
							transformer.setOutputProperty(OutputKeys.INDENT, "yes")
							transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

							StringWriter().use {
								val result = StreamResult(it)
								StringReader(xml).use {
									transformer.transform(StreamSource(it), result)
									result.getWriter().use {
										log.error("${resp.status()} / ${resp} ${it}")
									}
								}
							}
						}
					}
			}
			.block()
	}

	private fun writeGoogleStorage(fileBuffer: ByteBuffer, path: String, fileName: String){
		throw UnsupportedOperationException()
		// TODO
	}

}
