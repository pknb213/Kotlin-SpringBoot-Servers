package com.example.payment_mini_project.domain.payment

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentRepository: ReactiveMongoRepository<Payment, String>
