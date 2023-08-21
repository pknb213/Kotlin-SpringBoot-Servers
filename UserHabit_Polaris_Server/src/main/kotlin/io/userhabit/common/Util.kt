package io.userhabit.common

import com.google.gson.*
import org.bson.Document
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import reactor.util.Loggers
import java.io.*
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath
import kotlin.streams.toList

object Util {
	private val log = Loggers.getLogger(this.javaClass)
//	private val gson = Gson()
// TODO 아래 로직 작동안한다 이상하다...
//	private val gson = GsonBuilder().registerTypeAdapter(java.lang.Double::class.java, object : JsonSerializer<Double> {
//    override fun serialize(src: Double, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
//        return if (src == src.toLong().toDouble()) JsonPrimitive(src.toLong()) else JsonPrimitive(src)
//    }
//	}).create()
// TODO Gson()을 사용하면 json number 값을 파싱할 때 지수로 표현한다. (ex 1640050762904 >> 1.640050762904E12)
	private val gson = GsonBuilder()
    .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
    .create()
	private val gsonPretty = GsonBuilder().setPrettyPrinting().create()
	private val classMap = mutableMapOf<String, Any>().javaClass
	private val classList = mutableListOf<Map<String, Any>>().javaClass
	val hostName = InetAddress.getLocalHost().getHostName()

	fun toJsonString(list: Any): String {
		return gson.toJson(list)
	}

	fun toMapJsonString(listMap: List<MutableMap<String, Any>>?): String {
		return gson.toJson(listMap)
	}

	fun jsonToMap(json: String): Map<String, Any> {
		return gson.fromJson(json, classMap)
	}

	fun jsonToMutableMap(json: String): MutableMap<String, Any> {
		return gson.fromJson(json, classMap)
	}

	fun jsonToList(json: String): List<Map<String, Any>> {
		return gson.fromJson(json, classList)
	}

	fun jsonToMap(json: ByteArray): Map<String, Any> {
		return gson.fromJson(String(json), classMap)
	}

	fun jsonToList(json: ByteArray): List<Map<String, Any>> {
		return gson.fromJson(String(json), classList)
	}

	fun toPretty(json: Any): String {
		return gsonPretty.toJson(json)
	}

	fun <R> getResource(path: String, func: (Path) -> R): R {
		val uri = ClassLoader.getSystemResource(path).toURI()
		return	if(uri.scheme == "jar") FileSystems.newFileSystem(uri, Collections.emptyMap<String, Object>()).use{
			func(it.getPath("/${path}"))
		}
		else func(uri.toPath()) as R
	}

	fun <R> getResourceList(path: String, func: (Path) -> R): List<R> {
		val uri = ClassLoader.getSystemResource(path).toURI()
		return if(uri.scheme == "jar") FileSystems.newFileSystem(uri, Collections.emptyMap<String, Object>()).use {
			val p = it.getPath("/${path}")
			Files.walk(p, 1)
				.filter { !it.isDirectory() }
				.map {
					func(it)
				}
				.toList()
		}
		else uri.toPath().toFile().listFiles().filter { !it.isDirectory }.map { func(it.toPath()) }
	}

	// ByteBuffer를 InputStream으로 변환한는 적당한 라이브러리가 없어서 아래 로직 사용함. 왜 없는거야...
	private class ByteBufferBackedInputStream(var buf: ByteBuffer) : InputStream() {
		override fun read(): Int {
			return if (!buf.hasRemaining()) -1 else buf.get().toInt().and(0xFF)
		}

		override fun read(bytes: ByteArray, off: Int, len: Int): Int {
			var len = len
			if (!buf.hasRemaining()) return -1

			len = Math.min(len, buf.remaining())
			buf[bytes, off, len]
			return len
		}
	}

	fun toInputStream(buffer: ByteBuffer): InputStream {
		return ByteBufferBackedInputStream(buffer)
	}

	private val jsonWriter = JsonWriterSettings.builder().outputMode(JsonMode.SHELL).indent(true).build()
	fun toJsonQuery(jsonList: List<Document>, coll: String = ""): String {
		return jsonList.map { it.toJson(jsonWriter) } .joinToString(",\n").let {
			if(coll.isNotEmpty()) "db.${coll}.aggregate([${it}])"  else "[${it}]"
		}
	}

	@Throws(IOException::class)
	fun copy(`is`: InputStream, os: OutputStream) {
		val buffer = ByteBuffer.allocate(1024)
		val cOut = Channels.newChannel(os)
		val cIn = Channels.newChannel(`is`)
		while (cIn.read(buffer) != -1) {
			buffer.flip()
			while (buffer.hasRemaining()) {
				cOut.write(buffer)
			}
			buffer.clear()
		}
	}

	private val algorithm = "HmacSHA256"
	private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
	private val header = base64UrlEncoder.encodeToString("""{"typ":"JWT","alg":"HS256"}""".toByteArray())

	fun encodeJWT(payload: String, secret: String): String {
		val p = base64UrlEncoder.encodeToString(payload.toByteArray())
		return "${header}.${p}.${encodeSign(p, secret)}"
	}

	fun encodeSign(payload: String, secret: String): String {
		val mac = Mac.getInstance(algorithm)
		mac.init(SecretKeySpec(secret.toByteArray(), algorithm))
		return base64UrlEncoder.encodeToString(mac.doFinal("${header}.${payload}".toByteArray()))
	}

	fun sign(payload: String, secret: ByteArray): ByteArray {
		val mac = Mac.getInstance(algorithm)
		mac.init(SecretKeySpec(secret, algorithm))
		return mac.doFinal(payload.toByteArray(Charset.defaultCharset()))
	}

	//	eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6InRlc3QwMUB1c2VyaGFiaXQuaW8ifQ.hz9LQkBJJG1PIyZc3u-VGr0hoUthUP9peFn2Y314FXo
	val base64UrlDecoder = Base64.getUrlDecoder()!!

	private val sha = MessageDigest.getInstance("SHA-256")

	fun sha256Base64(src: String): String {
		sha.update(src.toByteArray())
		return base64UrlEncoder.encodeToString(sha.digest())
	}

	fun sha256(src: ByteArray): ByteArray {
		sha.update(src)
		return sha.digest()
	}

	fun toHex(data: ByteArray): String {
		return data.joinToString("") {
			val hex = Integer.toHexString(it.toInt())
			if (hex.length == 1) {
				"0${hex}"
			} else if (hex.length == 8) {
				hex.substring(6)
			} else {
				hex
			}
		}.lowercase(Locale.getDefault())
	}

	private val base64Encoder = Base64.getEncoder()
	fun base64Encode(src: String): String {
		return base64Encoder.encodeToString(src.toByteArray())
	}
}
