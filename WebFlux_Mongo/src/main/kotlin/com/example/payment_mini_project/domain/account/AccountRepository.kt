package com.example.payment_mini_project.domain.account

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: ReactiveMongoRepository<Account, String>