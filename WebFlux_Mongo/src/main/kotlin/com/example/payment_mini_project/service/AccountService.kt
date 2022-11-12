package com.example.payment_mini_project.service

import com.example.payment_mini_project.domain.account.Account
import com.example.payment_mini_project.domain.account.AccountRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AccountService (private val accountRepository: AccountRepository){
    fun insert(account: Account): Mono<Account> = accountRepository.insert(account)
    fun deleteAll(): Mono<Void> = accountRepository.deleteAll()
}