package com.example.payment_mini_project.handler

import com.example.payment_mini_project.domain.account.Account
import com.example.payment_mini_project.service.CsvFileUploadService
import com.example.payment_mini_project.utils.Response
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Paths

@Component
class CsvFileUploadHandler (private val csvFileUploadService: CsvFileUploadService){
    fun insertCsv(request: ServerRequest): Mono<ServerResponse> {
        val filename = request.queryParam("file").get()
        return Mono
            .fromCallable {
                filename
            }.flatMap {
                csvFileUploadService.insertObjectFromCsv(filename)
            }.flatMap {
                Response.okResponse()
            }.onErrorResume {
                Response.errorResponse(it)
            }
    }
}