package com.example.payment_mini_project.handler

import com.example.payment_mini_project.domain.group.Group
import com.example.payment_mini_project.service.GroupService
import com.example.payment_mini_project.utils.Response
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class GroupHandler (private val groupService: GroupService){
    fun insertGroupData(request: ServerRequest): Mono<ServerResponse> =
        request.bodyToMono(Group::class.java)
            .flatMap {
                groupService.insert(it)
            }.flatMap {
                Response.okResponse()
            }.onErrorResume {
                Response.errorResponse(it)
            }

    fun findGroupData(request: ServerRequest): Mono<ServerResponse>{
        var params = request.queryParams().toMap()["groupId"]?: emptyList()
        if (params.isNotEmpty()) params = params[0].split(",")
        return Flux
            .fromIterable(params)
            .defaultIfEmpty("")
            .flatMap {
                if (params.isEmpty()) groupService.findAll()
                else groupService.find(it)
            }
            .collectList()
            .flatMap {
                ServerResponse.ok().body(BodyInserters.fromValue(
                    it
                ))
            }.onErrorResume {
                Response.errorResponse(it)
            }
    }

    fun deleteGroupData(request: ServerRequest): Mono<ServerResponse> {
        var params = request.queryParams().toMap()["groupId"]?: emptyList()
        if (params.isNotEmpty()) params = params[0].split(",")

        return Flux
            .fromIterable(params)
            .defaultIfEmpty("")
            .flatMap {
                if (params.isEmpty()) error("Checked groupId url parameter")
                else groupService.delete(it)
            }
            .collectList()
            .flatMap {
                Response.okResponse()
            }.onErrorResume {
                Response.errorResponse(it)
            }
    }

    fun findGroupStatisticsData(request: ServerRequest): Mono<ServerResponse> {
        val groupId = request.queryParam("groupId").get()
        return Mono
            .fromCallable {
                groupId
            }
            .flatMap {
                groupService.decodeCondition(it)
            }
            .flatMap {
                groupService.aggregateByConditions(it)
            }
            .flatMap {
                it.first()["groupId"] = groupId
                ServerResponse.ok().body(BodyInserters.fromValue(
                    it.first()
                ))
            }.onErrorResume {
                Response.errorResponse(it)
            }
    }
}