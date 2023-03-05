package com.example.springbootrestserver.util

import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories(basePackageClasses = [
//    PaymentRepository::class,
//    AccountRepository::class,
//    GroupRepository::class
])
class MongoConfig: AbstractReactiveMongoConfiguration() {
 override fun getDatabaseName() = "test"
    override fun reactiveMongoClient() = mongoClient()
    @Bean
    fun mongoClient() = MongoClients.create()
    @Bean
    override fun reactiveMongoTemplate(
        databaseFactory: ReactiveMongoDatabaseFactory,
        mongoConverter: MappingMongoConverter
    ): ReactiveMongoTemplate {
        val mapper = DefaultMongoTypeMapper(null)
        mongoConverter.setTypeMapper(mapper)
        return ReactiveMongoTemplate(mongoClient(), databaseName)
    }
}
