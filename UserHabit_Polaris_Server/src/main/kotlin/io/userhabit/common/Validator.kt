package io.userhabit.common

import java.lang.Exception
import java.text.SimpleDateFormat

/**
 * 데이터 유효성 검증
 *
 * @author nsb
 */
class Validator {
	private var value: Any? = null
	private lateinit var key: String
	private val vList =  mutableListOf<Map<String, String>>()


	/**
	 * 데이터 유효성 검증 새로 시작
	 */
	fun new(value: Any?, key: String): Validator{
		this.value = value
		this.key = key
		return this
	}

	/**
	 * 데이터 유효성 검증 새로 시작
	 */
	fun new(value: Map<*, *>, key: String): Validator{
		this.value = value[key]
		this.key = key
		return this
	}

	/**
	 * null이면 패스
	 * 숫자형이면 크기
	 * 문자형이면 문자길이
	 */
	fun min(v: Int): Validator{
		val value = this.value ?: return this
		if((value !is Number && value !is String) || (value is Number && value.toInt() < v) || (value is String && value.length < v)){
			this.vList.add(mapOf(this.key to "Must be greater than or equal to $v"))
		}
		return this
	}

	/**
	 * null이면 패스
	 * 숫자형이면 크기
	 * 문자형이면 사이즈
	 */
	fun max(v: Int): Validator{
		val value = this.value ?: return this
		if((value !is Number && value !is String) || (value is Number && value.toInt() > v) || (value is String && value.length > v)){
			this.vList.add(mapOf(this.key to "Must be less than or equal to $v"))
		}
		return this
	}

	/**
	 * 숫자여야 함
	 * null이면 패스
	 */
	fun number() : Validator{
		val value = this.value ?: return this
		if(value !is Number){
			this.vList.add(mapOf(this.key to "Must be number type"))
		}
		return this
	}

	/**
	 * 같아야 됨
	 * null이면 패스
	 */
	fun must(v: Boolean) : Validator{
		val value = this.value ?: return this
		if((value != v)){
			this.vList.add(mapOf(this.key to "Must be $v"))
		}
		return this
	}

	/**
	 * 같아야 됨
	 * null이면 패스
	 */
	fun custom(v: Any, message: String) : Validator{
		val value = this.value ?: return this
		if((value != v)){
			this.vList.add(mapOf(this.key to message))
		}
		return this
	}

	/**
	 * 공백이나 빈 리스트가 아니여야 됨
	 * null이면 패스
	 */
	fun required(): Validator{
		val value = this.value
		if(value == null || (value is String && value.isEmpty()) || (value is List<*> && value.isEmpty())){
			this.vList.add(mapOf(this.key to "is required"))
		}
		return this
	}

	/**
	 * 권한 검사
	 * null, String 이면 패스
	 */
	fun hasSetPermission(adminLevel: Long): Validator{
		val value = this.value ?: return this
		if(value !is Number || (!Level.hasSetPermission(adminLevel, value.toLong())) ){
			this.vList.add(mapOf(this.key to "You don't have permission [hasSetPermission()]"))
		}
		return this
	}

	/**
	 * 하나라도 포함되어 있으면 true, 아니면 false
	 * null, String 이면 패스
	 */
	fun containsPermission(vararg levels: Long): Validator{
		val value = this.value ?: return this
		if(value !is Number || (!Level.contains(value.toLong(), *levels)) ){
			this.vList.add(mapOf(this.key to "You don't have permission [containsPermission()]"))
		}
		return this
	}

	/**
	 * 값이 있는 경우 권한 없음
	 * null이면 패스
	 */
	fun ifExistDenied(): Validator{
		val value = this.value ?: return this
		if(value !is String || value.isNotEmpty()){
			this.vList.add(mapOf(this.key to "You don't have permission [ifExistDenied()]"))
		}
		return this
	}

	/**
	 * 리스트 검사
	 * null이면 패스
	 */
	fun list(): Validator{
		val value = this.value ?: return this
		if(value !is List<*> || value.isEmpty()){
			this.vList.add(mapOf(this.key to "It is required"))
		}
		return this
	}

	/**
	 * 목록 중에 값이 있는지 검사
	 * null이면 패스
	 */
	fun required(ex: List<Any>): Validator{
		this.value ?: return this
		if(!ex.contains(this.value)){
			this.vList.add(mapOf(this.key to "It must be included ex:${ex}"))
		}
		return this
	}

	private val regexEmail = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
	/**
	 * 이메일 형식 검사
	 * null이면 패스
	 */
	fun email(): Validator{
		val v = this.value ?: return this
		if(v !is String || !regexEmail.matches(v)){
			this.vList.add(mapOf(this.key to "Invalid email format"))
		}
		return this
	}

//	val regexPassword = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,}\$")
	val regexPassword = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@\$!%*#?&]{2,}\$")
	/**
	 * 패스워드 형식 검사
	 * null이면 패스
	 */
	fun password(): Validator{
		val v = this.value ?: return this
		if(v !is String || !regexPassword.matches(v)){
			this.vList.add(mapOf(this.key to "Invalid value, at least one letter, one number and special character can be [@\$!%*#?&]"))
		}
		this.min(8)
		return this
	}

	val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
	/**
	 * 날짜 형식 검사 (ISO 8601 = "yyyy-MM-dd'T'HH:mm:ssX")
	 * null이면 패스
	 */
	fun date(): Validator{
		val v = this.value ?: return this
		try {
			isoDateFormatter.parse(v as String)
		}catch (ex: Exception){
			this.vList.add(mapOf(this.key to "Invalid date format. [${isoDateFormatter.toPattern()} != ${v}]"))
		}
		return this
	}

	// TODO delete
//	fun jsonObject(): Validator{
//		val v = this.value as? String
//		if(!v.isNullOrEmpty() && !Util.isValidJsonObject(v)){
//			this.vList.add(mapOf(this.key to "Invalid json object format"))
//		}
//		return this
//	}
//
//	fun jsonArray(): Validator{
//		val v = this.value as? String
//		if(!v.isNullOrEmpty() && !Util.isValidJsonArray(v)){
//			this.vList.add(mapOf(this.key to "Invalid json array format"))
//		}
//		return this
//	}
//
	fun jsonWebToken(secretKey: String): Validator{
		val v = this.value as String
		if(!isValidJWT(v, secretKey)){
			this.vList.add(mapOf(v to Message.get("en", Message.badRequest, "JWT")))
		}
		return this
	}

	fun isNotValid(): Boolean{
		return this.vList.size > 0
	}

	fun toExceptionList(): List<Map<String, String>> {
		return vList
	}

	companion object{
		fun isValidJWT(token: String, secretKey: String): Boolean {
			val tokens = token.split(".")
			return token.isNotEmpty() && tokens.size > 2 && tokens[2] == Util.encodeSign(tokens[1], secretKey)
		}
	}
}
