package com.example.payment_mini_project.domain.group

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository: ReactiveMongoRepository<Group, String>