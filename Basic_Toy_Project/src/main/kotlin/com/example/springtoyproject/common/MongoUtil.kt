package com.example.springtoyproject.common

import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.Document
import reactor.util.Loggers
import java.nio.file.Path

object MongoUtil {
    // Todo: Config.properties로 분리.
    private val log = Loggers.getLogger(this.javaClass)
    val SAAS_DB_NAME = "userhabit"
    private val client by lazy {
        val addresses = "127.0.0.1"
        val replicaSet = "mongodb.replicaSet1,2,3"
        val readPreference = ""
        if (addresses.contains(",")) MongoClients.create("mongodb://${addresses}/?replicaSet=${replicaSet}&readPreference=${readPreference}")
        else MongoClients.create("mongodb://${addresses}")
    }

    private val db by lazy {
        client.getDatabase(SAAS_DB_NAME)
    }

    fun getCollection(collectionName: String): MongoCollection<Document> {
        return db.getCollection(collectionName)
    }

    init {
        val initColl = System.getProperty("initcoll")
    } // end of init()
}