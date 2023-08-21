package io.userhabit.common

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.userhabit.polaris.dao.EmailTemplateDao
import org.bson.types.ObjectId
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.util.Loggers
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import org.bson.Document as D

/**
 * @author sbnoh
 */
object Email {
	private val log = Loggers.getLogger(this.javaClass)
	val COLLECTION_NAME = "mail"
	private val httpClient = HttpClient
		.create(ConnectionProvider.builder("Email")
			.maxConnections(10)
			.build())
		.compress(true)
	private val helpEmail = Config.get("email.help_email")
	private val helpName = Config.get("email.help_name")
	private val brunchUrl = Config.get("email.brunch_url")
	private val facebookUrl = Config.get("email.facebook_url")
	private val linkedinUrl = Config.get("email.linkedin_url")
	private val scope = listOf(
		"https://mail.google.com/",
		"https://www.googleapis.com/auth/gmail.addons.current.action.compose",
		"https://www.googleapis.com/auth/gmail.compose",
		"https://www.googleapis.com/auth/gmail.modify",
		"https://www.googleapis.com/auth/gmail.send"
	).joinToString(" ")
	private val privateKeyPath = Config.get("email.private_key_path")
	private val path = Paths.get(privateKeyPath)
	private lateinit var privateKeyId: String
	private lateinit var clientEmail: String
	private lateinit var sign: Algorithm

	init {
		if(path.toFile().exists()){
			val keyMap = Util.jsonToMap(Files.readString(path))
			privateKeyId = keyMap["private_key_id"] as String
			clientEmail = keyMap["client_email"] as String
			val privateKey = keyMap["private_key"] as String
			val pkcs8Pem = Base64.getDecoder().decode(privateKey
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replace(Regex("\\s+"), "")
			)
			sign = Algorithm.RSA256(
				null,
				KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkcs8Pem)) as RSAPrivateKey
			)
		}else{
			privateKeyId = ""
			clientEmail = ""
			sign = Algorithm.none()
		}
	}

	private var expireTime = 0L
	private var token = ""
	private var isRefreshing = false
	private val regexVariable = Regex("(\\*\\|.+?\\|\\*)")
	private val varMapGlobal = mapOf(
		"*|ADDRESS|*" to Config.get("email.address_kr"),
	)

	fun sendAuthentication(lang: String,toEmail: String, toName: String): Mono<Map<String, Any>> {
		val authKey = UUID.randomUUID().toString()
		val now = LocalDateTime.now()
		val r = LocalDateTime.now().plusMonths(3).withHour(23).withMinute(59) // TODO 기간 정함
		val emailVar = mapOf(
			"EMAIL_SUBJECT" to "[유저해빗] 이메일 인증을 진행해주세요",
			"*|TITLE|*" to "이메일 인증을 진행해주세요",
			"*|CONTENTS|*" to "안녕하세요. ${toName} 고객님.<br><br> " +
				"아래 버튼을 클릭하여 이메일을 인증하고 회원가입을 완료하세요. <br><br>" +
				"인증 유효기간은 <b>${r.year}년 ${r.monthValue}월 ${r.dayOfMonth}일 ${r.hour}시 ${r.minute}분</b> 까지 입니다. <br><br>" +
				"감사합니다.",
			"*|HELP_EMAIL|*" to helpEmail,
			"*|BRUNCH_URL|*" to brunchUrl,
			"*|FACEBOOK_URL|*" to facebookUrl,
			"*|CURRENT_YEAR|*" to now.year.toString(),
			// custom
			"*|BUTTON_AREA_STYLE|*" to "",
			"*|BUTTON_NAME|*" to "인증하기",
			// TODO url
			"*|BUTTON_URL|*" to "${Config.get("http.host")}:${Config.get("http.port")}/v3/user/auth_email?id=${URLEncoder.encode(toEmail, "UTF-8")}&key=${URLEncoder.encode(authKey, "UTF-8")}",
		)

		// TODO 현재는 ko 외엔 EN
		return send(toEmail, toName, "COMMON_${if(lang  == "ko") lang.uppercase() else "EN"}", emailVar)
			.map {
				it.plus("auth_key" to authKey)
			}
	}

	fun sendPlanChange(lang: String, toEmail: String, toName: String, fromPlan: String, toPlan: String): Mono<Map<String, Any>> {
		val now = LocalDateTime.now()
		val emailVar = mapOf(
			// common
			"EMAIL_SUBJECT" to "[유저해빗] 플랜변경 신청이 완료 되었습니다",
			"*|TITLE|*" to "플랜변경 신청이 완료 되었습니다",
			"*|CONTENTS|*" to "안녕하세요. ${toName} 고객님.<br><br> " +
				"플랜변경 신청(‘${fromPlan}’에서 ‘${toPlan}’)이 완료 되었습니다. <br><br> " +
				"1영업일 이내에 담당자가 연락드리고 바로 처리 도와드리겠습니다. <br><br> " +
				"감사합니다.",
			"*|HELP_EMAIL|*" to helpEmail,
			"*|BRUNCH_URL|*" to brunchUrl,
			"*|FACEBOOK_URL|*" to facebookUrl,
			"*|CURRENT_YEAR|*" to now.year.toString(),
			// custom
			"*|BUTTON_AREA_STYLE|*" to "",
			"*|BUTTON_NAME|*" to "진행 사항 확인하기",
			// TODO url
			"*|BUTTON_URL|*" to "${Config.get("http.host")}:${Config.get("http.port")}/plan_progress?id=${URLEncoder.encode(toEmail, "UTF-8")}&key=${URLEncoder.encode("abcd", "UTF-8")}",
		)

		return send(toEmail, toName, "COMMON_${if(lang  == "ko") lang.uppercase() else "EN"}", emailVar)
	}

	fun sendPlanDelete(lang: String, toEmail: String, toName: String): Mono<Map<String, Any>> {
		val now = LocalDateTime.now()
		val emailVar = mapOf(
			"EMAIL_SUBJECT" to "[유저해빗] 플랜해지 처리가 완료 되었습니다",
			"*|TITLE|*" to "플랜해지 처리가 완료 되었습니다",
			"*|CONTENTS|*" to "안녕하세요. ${toName} 고객님.<br><br> " +
				"고객님께서 요청하신 플랜해지 처리가 완료되었습니다.<br><br> " +
				"감사합니다.",
			"*|HELP_EMAIL|*" to helpEmail,
			"*|BRUNCH_URL|*" to brunchUrl,
			"*|FACEBOOK_URL|*" to facebookUrl,
			"*|CURRENT_YEAR|*" to now.year.toString(),
			// custom
			"*|BUTTON_AREA_STYLE|*" to "display:none;",
		)

		return send(toEmail, toName, "COMMON_${if(lang  == "ko") lang.uppercase() else "EN"}", emailVar)
	}

	fun sendInviteMember(lang: String, authKey: String, fromEmail: String, fromName: String, toEmail: String, toName: String): Mono<Map<String, Any>> {
		val now = LocalDateTime.now()
		val emailVar = mapOf(
			"EMAIL_SUBJECT" to "[유저해빗] 회원가입을 초대합니다",
			"*|TITLE|*" to "회원가입 초대",
			"*|CONTENTS|*" to "안녕하세요.<br><br> " +
				"${fromName}(${fromEmail}) 고객님께서 회원가입 초대 메일을 발송하였습니다.<br><br> " +
				"아래 '가입하기' 버튼을 눌러 가입을 진행하세요.<br><br> " +
				"감사합니다.",
			"*|HELP_EMAIL|*" to helpEmail,
			"*|BRUNCH_URL|*" to brunchUrl,
			"*|FACEBOOK_URL|*" to facebookUrl,
			"*|CURRENT_YEAR|*" to now.year.toString(),
			// custom
			"*|BUTTON_AREA_STYLE|*" to "",
			"*|BUTTON_NAME|*" to "가입하기",
			"*|BUTTON_URL|*" to "${Config.get("http.host")}:${Config.get("http.port")}/register?key=${URLEncoder.encode(authKey, "UTF-8")}",
		)

		return send(toEmail, toName, "COMMON_${if(lang  == "ko") lang.uppercase() else "EN"}", emailVar)
	}

	/**
	 * 최초 구글 이메일 API 사용, 등록 절차
	 * 1. 서비스 어카운트 생성 [https://console.cloud.google.com/apis/credentials]
	 * 2. 어카운트에서 키 생성, 키 다운로드 [https://console.cloud.google.com/iam-admin/serviceaccounts]
	 * 3. 새로운 아이디에서 서비스 어카운트에 모든 권한 위임  [https://admin.google.com/u/2/ac/owl/domainwidedelegation]
	 *  - Client ID 항목에 보안 키에서 client id 가져와서 입력
	 *  - OAuth2 scopes 항목에 아래 서비스를 등록해야 함
	 *    https://mail.google.com/,https://www.googleapis.com/auth/gmail.addons.current.action.compose,https://www.googleapis.com/auth/gmail.compose,https://www.googleapis.com/auth/gmail.modify,https://www.googleapis.com/auth/gmail.send
	 *
	 * 보내는 계정 관리를 새로운 계정으로 이전
	 *  - 수정 > config.*.properties / email.help_email=새로운이메일계정
	 *  - 새로운 계정으로 로그인 한 뒤 위에 3번 항목부터 실행
	 *
	 * 구글 이메일 전송 제한 / 하루 2,000개 [https://support.google.com/a/answer/166852?hl=ko#]
	 */
	private fun send(toEmail: String, toName: String, templateId: String, varMapArg: Map<String, String> = mapOf()): Mono<Map<String, Any>> {
		val issueTime = System.currentTimeMillis()
		return if(issueTime < expireTime - (60 * 1000) || isRefreshing){ // 1시간에 한번 갱신. 1분 미리 갱신함
			// get buffered token
			Mono.just(token)
		}else{
			expireTime = issueTime + 3600 * 1000L // 1시간 추가, google api 만료 시간이 1시간 기본값
			isRefreshing = true

			// make jwt
			val jwt = JWT.create()
				.withKeyId(privateKeyId)
				.withIssuer(clientEmail)
				.withSubject(helpEmail)
				.withAudience("https://oauth2.googleapis.com/token")
				.withIssuedAt(Date(issueTime))
				.withExpiresAt(Date(expireTime))
				.withClaim("scope", scope)
				.sign(sign)

			val body = "grant_type=${URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8")}" +
				"&assertion=${jwt}"

			// get token
			httpClient
				.headers { it.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED) }
				.post().uri("https://oauth2.googleapis.com/token")
				.send(ByteBufFlux.fromString(Mono.just(body)))
				.responseContent()
				.aggregate()
				.asString()
				.flatMap {
					val resultMap = Util.jsonToMap(it)
					val error = resultMap["error"]
					if(error != null){
						Mono.error(Exception(error.toString()))
					}else{
						token = resultMap["access_token"] as String
						isRefreshing = false
						Mono.just(token)
					}

				}
				.onErrorMap {
					isRefreshing = false
					it
				}
		}
			.flatMap { token ->
				// get email template
				val template =  EmailTemplateDao.getTemplate(templateId)
					// make email template
					.map { doc ->
						val varMap = varMapGlobal + varMapArg
						// 템플릿 내 변수를 정규식으로 찾아서 찾은 값을 varMap 변수의 키 값으로 치환
						val html = regexVariable.replace(doc["html"] as String) { varMap[it.value] ?: "" }
						val rawForm = """
Content-Type: text/html; charset="UTF-8"
From: =?UTF-8?B?${Util.base64Encode(helpName)}?= <${helpEmail}>
To: =?UTF-8?B?${Util.base64Encode(toName)}?= <${toEmail}>
Subject: =?UTF-8?B?${Util.base64Encode(varMap["EMAIL_SUBJECT"] as String)}?=

${html}
						""".trimIndent() // 뉴라인 수정하지 말 것 (RFC 2822 / https://tools.ietf.org/html/rfc2822#appendix-A.1)

						"""{ "raw": "${Util.base64Encode(rawForm)}" }"""
					}

				// send email
				httpClient
					.headers {
						it.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
						it.set(HttpHeaderNames.AUTHORIZATION, "Bearer ${token}")
					}
					.post()
					.uri("https://content-gmail.googleapis.com/gmail/v1/users/me/messages/send")
					.send(ByteBufFlux.fromString(template))
					.responseContent()
					.aggregate()
					.asString()
					.flatMap {
						val resultMap = Util.jsonToMap(it)
						val error = resultMap["error"]
						val doc = D()
							.append("type", "gmail") // TODO
							.append("email_id", resultMap["id"])
							.append("thread_id", resultMap["threadId"])
							.append("from", helpEmail)
							.append("to", toEmail)
							.append("template_id", templateId)
							.append("created_date", Date())
						if(error != null){
							error as Map<*, *>
							Exception(error.toString()).printStackTrace()
							doc.append("code", error["code"])
								.append("message", error["message"])
								.append("error_stacks", error["errors"])
						}

						Mono
							.from(MongodbUtil.getCollection(COLLECTION_NAME).insertOne(doc))
							.map {resultMap }
					}
			}
	} // end of send()

}
