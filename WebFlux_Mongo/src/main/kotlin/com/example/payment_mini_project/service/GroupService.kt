package com.example.payment_mini_project.service

import com.example.payment_mini_project.domain.group.Group
import com.example.payment_mini_project.domain.group.GroupRepository
import com.example.payment_mini_project.mongoConfig.MongoConfig
import org.bson.Document
import org.springframework.data.domain.Example
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import com.example.payment_mini_project.utils.Condition as C

@Service
class GroupService (
    private val groupRepository: GroupRepository,
    private val mongo: MongoConfig){

    fun insert(group: Group): Mono<Group> = groupRepository.insert(group)

    fun findAll(): Flux<Group> = groupRepository.findAll()

    fun find(id: String): Mono<Group> = groupRepository.findOne(Example.of(Group(groupId = id.toInt())))

    fun delete(id: String): Mono<Void> =
        groupRepository.findOne(Example.of(Group(groupId = id.toInt())))
            .flatMap {
                groupRepository.delete(it)
            }

    fun decodeCondition(id: String): Mono<MutableList<Map<String, String>>> =
        groupRepository.findOne(Example.of(Group(groupId = id.toInt())))
            .defaultIfEmpty(Group(groupId = 0, condition = ""))
            .flatMap {
                if (id.toInt() != it.groupId) return@flatMap Mono.error(RuntimeException("Invalid Url Argument: groupId"))
                else {
                    var resList = mutableListOf<Map<String, String>>()
                    val conditions = it.condition?.replace(" ", "").toString()
                    val keys = Regex("(?<=key:)[a-z|A-Z\\s!]+").findAll(conditions)
                    val operators = Regex("(?<=operator:)[a-z|A-Z\\s!]+").findAll(conditions)
                    val values = Regex("(?<=value:)[\\w\\s\\d\\[\\]\\,가-힣\$]+").findAll(conditions)
                    keys.zip(operators).zip(values).forEach {
                        resList.add(
                            mapOf(
                                "key" to it.first.first.value,
                                "operator" to it.first.second.value,
                                "value" to it.second.value)
                        )
                    }
//                    println("Decoding: $resList")
                    return@flatMap Mono.just(resList)
                }
            }

    fun aggregateByConditions(conditionList: MutableList<Map<String, String>>): Mono<List<Document>> {
        /**
         * @param conditonList: Group conditions
         */
        val beforeQryList = mutableListOf<Document>()
        val afterQryList = mutableListOf<Document>()
        var lookupLet = mutableMapOf("accountId" to "\$accountId")
//        println("Conditions: $conditionList")

        conditionList.forEach {
            val key = it["key"]
            val oper = it["operator"]!!
            var value = it["value"]!!

            // Create Match Query
            // List Type Value
            if (oper in listOf("in", "between", "not in")) {
                val valueList = mutableListOf<Any>()
                value.substring(1)
                    .split(",").onEach { element ->
                        if (element.toIntOrNull() != null) valueList.add(element.toInt())
                        else valueList.add(C.convertKr(element))
                    }
                if (key in listOf("age")) {
                    afterQryList.add(
                        Document("\$gte", listOf("\$" + key, valueList[0]))
                    )
                    afterQryList.add(
                        Document("\$lte", listOf("\$" + key, valueList[1]))
                    )
                }
                else beforeQryList.add(Document(key, mapOf(C.convertOperation(oper) to valueList)))
            }else{ // Not List
                if (key in listOf("amount", "accountId", "_id", "age")){
                    if (key in listOf("age")) {
                        afterQryList.add(
                            Document(C.convertOperation(oper), listOf("\$" + key, value.toInt()))
                        )
                    }
                    else beforeQryList.add(Document(key, mapOf(C.convertOperation(oper) to value.toInt())))
                }else{
                    value = C.convertKr(value)
                    if (value.first().toString() == "$") {
                        lookupLet.put(value.substring(1), value)
                        value = "\$" + value
                    }
                    if (key in listOf("residence")) {
                        afterQryList.add(
                            Document(C.convertOperation(oper), listOf("\$" + key, value))
                        )
                    }
                    else beforeQryList.add(Document(key, mapOf(C.convertOperation(oper) to value)))
                }
            }
        }

        val pipeline = mutableListOf<Document>()

        if (beforeQryList.size > 1) {
            pipeline.add(
                Document("\$match", mapOf("\$and" to beforeQryList))
            )
//            println("Before Qry: $beforeQryList")
        }
        else if (beforeQryList.size == 1) {
            pipeline.add(Document("\$match", beforeQryList.first()))
//            println("Before Qry: $beforeQryList")
        } else { }

//        println("After Qry: $afterQryList")

        if (afterQryList.isNotEmpty()){
            afterQryList.add(0, Document("\$eq", listOf("\$_id", "\$\$accountId")))
            val lookupQry = Document("\$lookup", mapOf(
                "from" to "account",
                "let" to lookupLet,
                "pipeline" to listOf(
                    mapOf("\$match" to mapOf(
                        "\$expr" to mapOf(
                            "\$and" to afterQryList
                                .map {
                                    it.map {
                                        it.key to it.value
                                    }.toMap()
                                }
                        )
                    ))
                ),
                "as" to "account"
            ))
            val unwindQry = Document("\$unwind", "\$account")
            pipeline.add(lookupQry)
            pipeline.add(unwindQry)
//            println("Lookup Qry: $lookupQry")
        }

        val projectQry = Document("\$project", mapOf(
            "_id" to 0,
            "totalAmount" to "\$totalAmount",
            "avgAmount" to mapOf("\$round" to listOf("\$avgAmount", 1)),
            "minAmount" to "\$minAmount",
            "maxAmount" to "\$maxAmount",
            "count" to "\$count"
//            "amount" to "\$amount",
//            "itemCategory" to "\$itemCategory",
//            "methodType" to "\$methodType",
//            "region" to "\$region",
//            "age" to "\$account.age",
//            "residence" to "\$account.residence",
        ))
        val groupQry = Document("\$group", mapOf(
            "_id" to null,
            "minAmount" to mapOf("\$min" to "\$amount"),
            "maxAmount" to mapOf("\$max" to "\$amount"),
            "avgAmount" to mapOf("\$avg" to "\$amount"),
            "totalAmount" to mapOf("\$sum" to "\$amount"),
            "count" to mapOf("\$sum" to 1 )
        ))
        pipeline.add(groupQry)
        pipeline.add(projectQry)
//        println("Pipeline: $pipeline")
        return Flux
            .from(
                mongo.mongoClient()
                    .getDatabase("test")
                    .getCollection("payment")
                    .aggregate(pipeline)
            )
//            .map {
//                println("~> $it")
//                it
//            }
            .collectList()
    }
}