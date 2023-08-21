package io.userhabit.common

import reactor.util.Loggers
import java.lang.RuntimeException

//class PolarisException(
//	message: String,
//	data: Any,
//) : Exception() {
//
//	companion object {
//		fun status400BadRequest(message: String, data: Any): Exception {
//
//			return PolarisException(message,
//				mapOf(
//					"status" to statusCode,
//					"message" to message,
//					"data" to data,
//					"field_list" to fieldList,
//				)
//			)
//		}
//	}
//}

class PolarisException(
	val data: Any,
	val messageCode: Int,
	val statusCode: Int,
	override val message: String = "",
): Exception(message) {

	private val log = Loggers.getLogger(this.javaClass)

	companion object{

		fun status201Created(messageCode: Int = Message.badRequest): PolarisException {
			return PolarisException("", messageCode, 201)
		}

		fun status304NotModified(messageCode: Int = Message.badRequest): PolarisException {
			return PolarisException("", messageCode, 304)
		}

		fun status400BadRequest(data: Any = "", messageCode: Int = Message.badRequest): Exception {
			return PolarisException(data, messageCode, 400,)
		}

		fun status401Unauthorized(data: Any = "", messageCode: Int = Message.unauthorized): PolarisException {
			return PolarisException(data, messageCode, 401)
		}

		fun status403Forbidden(data: Any = "", messageCode: Int = Message.forbidden): PolarisException {
			return PolarisException(data, messageCode, 403)
		}

		fun status404NotFound(messageCode: Int = Message.badRequest): PolarisException {
			return PolarisException("", messageCode, 404)
		}

		fun status409Conflict(messageCode: Int = Message.badRequest): PolarisException {
			return PolarisException("", messageCode, 409)
		}

		fun status410Gone(messageCode: Int = Message.badRequest): PolarisException {
			return PolarisException("", messageCode, 410)
		}
	}
}

//class PolarisException(
//	val data: Map<String, Any>,
//) : Exception() {
//
//	constructor(message: String, status: Int) : super(message) {
//	}
//	private constructor(status: Int) : super()
//	private constructor(message: String, cause: Throwable) : super(message, cause)
//	private constructor(cause: Throwable) : super(cause)
//}
