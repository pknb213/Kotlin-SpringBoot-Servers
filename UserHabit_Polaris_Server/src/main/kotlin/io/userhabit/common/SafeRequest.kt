package io.userhabit.common

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.Attribute
import io.netty.handler.codec.http.multipart.FileUpload
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import org.bson.Document
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.websocket.WebsocketInbound
import reactor.util.Loggers
import java.net.InetAddress
import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Null pointer exception 없는 안전한 Http, Websocket 통합 리퀘스트
 *
 * @author nsb
 */
class SafeRequest {
	private val log = Loggers.getLogger(this.javaClass)
	private lateinit var req: HttpServerRequest
	private lateinit var wsi: WebsocketInbound
	private lateinit var paramNew: Map<String, String>
	private lateinit var queryNew: Map<String, List<String>>
	private lateinit var bodyString: String
	private lateinit var file: Map<String, Map<String, Any>>
	private lateinit var bodyParam: Map<String, String>

	// TODO wsi
	private val param: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
		if(::paramNew.isInitialized) paramNew else req.params() ?: mapOf()
	}
	// TODO wsi
	private val query: Map<String, List<String>> by lazy(LazyThreadSafetyMode.NONE) {
		if(::queryNew.isInitialized) queryNew else QueryStringDecoder(req.uri()).parameters()
	}
	private val headers: HttpHeaders  by lazy(LazyThreadSafetyMode.NONE) {
		if(req != null) req.requestHeaders() else wsi.headers()
	}

	constructor(req: HttpServerRequest, byteBuf: ByteBuf) {
		this.req = req
//		log.info("1 ${byteBuf.refCnt()}")
		val contentType = headers.get(HttpHeaderNames.CONTENT_TYPE)

		if(contentType.isNullOrEmpty()){
			bodyString = byteBuf.toString(Charset.defaultCharset())
			bodyParam = mapOf()
			file = mapOf()
			byteBuf.release()
		}else if(contentType.startsWith(HttpHeaderValues.APPLICATION_JSON)){
			bodyString = byteBuf.toString(Charset.defaultCharset())
			bodyParam = if(bodyString.trim().first() == '{') Util.jsonToMap(bodyString) as Map<String, String> else mapOf()
			file = mapOf()
			byteBuf.release()
		}else if(contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED)){
			bodyString = byteBuf.toString(Charset.defaultCharset())
			bodyParam = URLDecoder.decode(bodyString, Charset.defaultCharset())
				.split("&").map { it.split("=") }.associateBy({it[0]},{it[1]})
			file = mapOf()
			byteBuf.release()
		}else if(contentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA)){
			val decoder = HttpPostMultipartRequestDecoder(
				DefaultFullHttpRequest(req.version(), req.method(), req.uri(), byteBuf, headers, headers)
			)

			val result = decoder.bodyHttpDatas
				.partition { it.httpDataType == InterfaceHttpData.HttpDataType.FileUpload }

			file = result.first.associateBy({it.name}, {
				it as FileUpload
				mapOf(
					"body" to it.content().nioBuffer(),
					"type" to it.contentType,
					"name" to it.filename)
			})

			bodyParam = result.second.associateBy({it.name}, {
				(it as Attribute).content().toString(Charset.defaultCharset())
			})
			bodyString = ""

			byteBuf.release()
			decoder.destroy()
		}else{
			bodyString = byteBuf.toString(Charset.defaultCharset())
			bodyParam = mapOf()
			file = mapOf()
			byteBuf.release()
		}
//		log.info("2 ${byteBuf.refCnt()}")
	}

	constructor(wsi:WebsocketInbound, byteBuf: ByteBuf) {
		this.wsi  = wsi
	}

	fun getNew(query: Map<String, List<String>>, param: Map<String, String>,
	           bodyString: String = "", bodyParam: Map<String, String> = mapOf(),
	           file: Map<String, Map<String, Any>> = mapOf()): SafeRequest {
		this.queryNew = query
		this.paramNew = param
		this.bodyString = bodyString
		this.bodyParam = bodyParam
		this.file = file
		return this
	}


	fun getParamOrDefault(key: String, defaultValue: String = ""): String {
		return param.getOrDefault(key, defaultValue)
	}

	fun getParamOrDefault(key: String, defaultValue: Int): Int {
		return try {
			param[key]?.toInt() ?: defaultValue
		}catch (ex: Exception){
			log.error(ex.toString(), ex)
			defaultValue
		}
	}

	fun splitParamOrDefault(key: String, defaultValue: String = ""): List<String> {
		return param.getOrDefault(key, defaultValue)
			.split(",")
			.filter { it.isNotEmpty() }
	}

	fun getQueryOrDefault(key: String, defaultValue: String = ""): String {
		return query.getOrDefault(key, listOf(defaultValue))[0]
	}

	fun getQueryOrDefault(key: String, defaultValue: Int): Int {
		return try{
			query[key]?.get(0)?.toInt() ?: defaultValue
		}catch (ex:Exception){
			//log.error(ex.toString(), ex)
			return defaultValue
		}
	}

	// TODO 제네릭
//	fun <V> getQueryOrDefault(key: String, defaultValue: V): V {
//		return try{
////			query.getOrDefault(key, listOf("")).get(0) as V
////			query.getOrDefault(key, listOf(defaultValue))[0] as V
//			mapOf("a" to listOf("1"), "b" to listOf(1)).getOrDefault(key, listOf(defaultValue))[0] as V
//		}catch (ex:Exception){
//			error(ex.toString())
//			defaultValue
//		}
//	}

	fun getQueryOrDefault(key: String, defaultValue: LocalDate) : LocalDateTime {
		return (query[key]?.get(0)?.let { LocalDate.parse(it, Companion.DATE_FORMAT) } ?: defaultValue) .atStartOfDay()
	}

	fun splitQueryOrDefault(key: String, defaultValue: String = ""): List<String> {
		return (query[key]?.get(0) ?: defaultValue)
			.split(",")
			.filter { it.isNotEmpty() }
	}

	fun getQueryOrDefaultToRegex(key: String, defaultValue: String = "") : String {
		return (query[key]?.get(0) ?: defaultValue)
			.split(",")
			.filter { it.isNotEmpty() }
			.joinToString (prefix = "\"", postfix = "\"", separator = "|")
	}

	fun getHeaderOrDefault(key: String, defaultValue: String = "") : String {
		return headers[key] ?: defaultValue
	}

	fun getBodyJsonToDocument(): Document {
		return Document.parse(bodyString)
	}

	fun getBodyOrDefault(key: String, defaultValue: String = ""): String {
		return bodyParam.getOrDefault(key, defaultValue)
	}

	fun getBodyOrDefault(key: String, defaultValue: Int): Int {
		return try {
			bodyParam[key]?.toInt() ?: defaultValue
		}catch (ex: Exception){
			log.error(ex.toString(), ex)
			defaultValue
		}
	}

	fun splitBodyOrDefault(key: String, defaultValue: String = ""): List<String> {
		return bodyParam.getOrDefault(key, defaultValue)
			.split(",")
			.filter { it.isNotEmpty() }
	}

	fun getBodyJsonToList(): List<Map<String, Any>> {
		return if(this.bodyString.isNotEmpty()) Util.jsonToList(this.bodyString) else listOf()
	}

	fun getBodyJsonToMap(): Map<String, Any>{
		return if(this.bodyString.isNotEmpty())Util.jsonToMap(this.bodyString) else mapOf()
	}

	fun getBodyJsonToMutableMap(): MutableMap<String, Any>{
		return if(this.bodyString.isNotEmpty())Util.jsonToMutableMap(this.bodyString) else mutableMapOf()
	}

	fun getBody(): String{
		return this.bodyString
	}

	fun getFile(key: String): Map<String, Any> {
		return file.getOrDefault(key, mapOf<String, Any>())
	}

	fun getIP(): String {
		return headers["X-Real-Ip"] ?: headers["X-Forwarded-For"] ?: req.remoteAddress().address.hostAddress
	}

	/**
	 * TODO 프런트엔드에서 수동으로 설정 가능하게
	 */
	val lang: String  by lazy(LazyThreadSafetyMode.NONE) {
		Locale.LanguageRange.parse(headers["Accept-Language"] ?: "en").first().range.substring(0, 2)
	}

	fun getInetAddress(): InetAddress {
		return try {
			headers["X-Real-Ip"]?.let { InetAddress.getByName(it) }
				?: headers["X-Forwarded-For"]?.let { InetAddress.getByName(it) }
				?: req.remoteAddress().address
		}catch (ex: Throwable){
			log.info(ex.toString(), ex) // 백엔드 에러가 아니기 때문에 info
			req.remoteAddress().address
		}
	}

	private val parsedToken: Map<String, Any> by lazy(LazyThreadSafetyMode.NONE) {
		val jwts = getJWT().split(".")
		if(jwts.size < 3)
			mapOf()
		else
			Util.jsonToMap(Util.base64UrlDecoder.decode(jwts[1]))
	}

	fun getUserId(): String{
		return parsedToken.getOrDefault("id", "") as String
	}

	@Suppress("UNCHECKED_CAST")
	fun getAppIdList(): List<String>{
		return parsedToken.getOrDefault("ail", "") as List<String>
	}

	fun getLevel(): Long{
		return (parsedToken.getOrDefault("lvl", 0L) as Number).toLong()
	}

	fun getJWT(): String{
		return getHeaderOrDefault("Authorization", "")
	}

	fun getCompanyID(): String{
		return parsedToken.getOrDefault("cid", "").toString()
	}

	fun method(): String{
		return this.req.method().name()
	}
//	override fun toString(): String {
//		return listOf(mapOf("param" to param), mapOf("query" to query), mapOf("body" to bodyParam), mapOf("file" to file), mapOf("header" to headers.toList()), ).toString()
//	}

	companion object {
		private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
		val emptyByteBuf = Unpooled.copiedBuffer("".toByteArray())
	}

    // Todo : Just Debug
    fun getThis(): String{
        return "REQ >> ${this.req}\n, Body: ${this.bodyString}\n, Header: ${this.headers}\n" +
                ", Param: ${this.param}\n, Query: ${this.query}\n"
    }
}
