package io.userhabit.common

import reactor.util.Loggers

/**
 * @author sbnoh
 * TODO message.json 파일 읽어서 처리 < 할까 말까 고민 중...
 */
object Message {
	private val log = Loggers.getLogger(this.javaClass)

	/** You don't have permission */
	const val forbidden= 403
	/** Invalid value */
	const val badRequest = 400
	/** Unauthorized */
	const val unauthorized = 401

	private val messageMap = mapOf(
		"en${forbidden }" to "You don't have permission",
		"ko${forbidden }" to "권한이 없습니다",
		"en${badRequest }" to "Invalid value",
		"ko${badRequest }" to "잘못된 값입니다",
		"en${unauthorized }" to "Unauthorized",
		"ko${unauthorized }" to "인증되지 않았습니다",
	)

	fun get(lang: String, code: Int): String {
		return messageMap["${lang}${code}"] ?: messageMap["en${code}"] as String
	}

	fun get(lang: String, code: Int, value: String): String {
		return "${get(lang, code)} [${value}]"
	}
}
