package com.example.payment_mini_project.service

import com.example.payment_mini_project.domain.account.Account
import com.example.payment_mini_project.domain.account.AccountRepository
import com.example.payment_mini_project.domain.group.Group
import com.example.payment_mini_project.domain.group.GroupRepository
import com.example.payment_mini_project.domain.payment.Payment
import com.example.payment_mini_project.domain.payment.PaymentRepository
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Paths

@Service
class CsvFileUploadService (
    private val accountRepository: AccountRepository,
    private val paymentRepository: PaymentRepository,
    private val groupRepository: GroupRepository){
    @Transactional
    fun insertObjectFromCsv(fileName: String): Mono<Any> {
        val filePath = Paths.get("${File.separatorChar}dummyCsv", "$fileName.csv")
        val resource: Resource = InputStreamResource(javaClass.getResourceAsStream(filePath.toString())!!)
        val reader = resource.inputStream.bufferedReader()
        return Mono.fromCallable {
            reader.lineSequence()
                .filter { it.isNotBlank() }
                .drop(1)
                .map {
                    if (fileName == "accounts") {
                        val (id, residence, age) =
                            it.split(',', ignoreCase = false, limit = 3)
                        accountRepository.insert(
                            Account(
                                accountId = id.toInt(),
                                residence = residence.trim(),
                                age = age.trim().removeSurrounding("\"").toInt()
                            )
                        ).subscribe()
                    }
                    else if(fileName == "payments") {
                        val splitString = it.split(',', ignoreCase = false, limit = 6)
                        paymentRepository.insert(
                            Payment(
                                paymentId = splitString[0].toInt(),
                                accountId = splitString[1].trim().toInt(),
                                amount = splitString[2].trim().toInt(),
                                methodType = splitString[3].trim(),
                                itemCategory = splitString[4].trim(),
                                region = splitString[5].trim().removeSurrounding("\"")
                            )
                        ).subscribe()
                    }
                    else if(fileName == "groups") {
                        val splitString = it.split('|', ignoreCase = false, limit = 3)
                        groupRepository.insert(
                            Group(
                                groupId = splitString[0].toInt(),
                                condition = splitString[1].trim(),
                                description = splitString[2].trim()
                            )
                        ).subscribe()
                    }
                    else throw NotFoundException()
                }.toList()
        }
    }
}