package com.example.springbootrestserver.global.common
import reactor.util.Loggers

object StatusResponse {
    private val log = Loggers.getLogger(this.javaClass)
    fun status200Ok(
        data: Any = "",
        message: String = "OK"
    ): Map<String, Any> {
        return custom(200, message, data)
    }

    fun status201Created(message: String = "Created"): Map<String, Any> {
        return custom(201, message)
    }

    fun status304NotModified(message: String = "Not Modified"): Map<String, Any> {
        return custom(304, message)
    }

    fun status400BadRequest(message: String = "Bad Request", data: Any = ""): Map<String, Any> {
        return custom(400, message, data)
    }

    fun status401Unauthorized(message: String = "Unauthorized"): Map<String, Any> {
        return custom(401, message)
    }

    fun status403Forbidden(message: String = "Forbidden"): Map<String, Any> {
        return custom(403, message)
    }

    fun status404NotFound(message: String = "Not Found"): Map<String, Any> {
        return custom(404, message)
    }

    fun status409Conflict(message: String = "Conflict"): Map<String, Any> {
        return custom(409, message)
    }

    fun status410Gone(message: String = "Gone"): Map<String, Any> {
        return custom(410, message)
    }

    fun status500InteralServerError(ex: Throwable): Map<String, Any> {
        return custom(500, ex)
    }

    fun custom(
        statusCode: Int,
        message: String,
        data: Any = ""
    ): Map<String, Any> {
        return mapOf(
            "status" to statusCode,
            "message" to message,
            "data" to data,
        )
    }

    fun custom(statusCode: Int, ex: Throwable): Map<String, Any> {
        return mapOf(
            "status" to statusCode,
            "message" to ex.toString(),
//            "throw" to if (Config.isDevMode) ex.stackTrace
//                .map { it.toString() }
//                .joinToString("\n") else ""
        )
    }
}