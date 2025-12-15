@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.core

import com.lerchenflo.schneaggchatv3server.core.security.HashEncoder
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.IndexInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * MainController for Ping etc.
 */

@RestController
class MainController(
    private val userService: UserService,
    private val hashEncoder: HashEncoder,
    private val mongoTemplate: MongoTemplate,
    @Value($$"${defaultaccount.password}") private val defaultPassword: String
){

    @GetMapping("/public/test")
    fun test(): String {
        return "Up and running!"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        //Code to execute on app start finished
        //listMongoIndexes()

        //Create default Account for Google play / App Store
        val defaultUserUserName = "TestAccount"
        val defaultUser = userService.findByUsername(defaultUserUserName)
        if(defaultUser == null){
            userService.save(
                User(
                    username = defaultUserUserName,
                    hashedPassword = hashEncoder.encode(defaultPassword),
                    email = "defaultuser@schneaggchat.com",
                    userDescription = "",
                    userStatus = "Default Test Account for Google Play / App store",
                    birthDate = "2000-01-01",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
            )
        }

    }

    fun listMongoIndexes() {
        val collections = mongoTemplate.db.listCollectionNames().toList()

        println("========= MongoDB Index Report =========")
        for (collection in collections) {
            println("Collection: $collection")

            val indexOps = mongoTemplate.indexOps(collection)
            val indexInfos: List<IndexInfo> = indexOps.indexInfo

            if (indexInfos.isEmpty()) {
                println("  -> No indexes found")
            } else {
                indexInfos.forEach { index ->
                    println("  -> Index: ${index.name}")
                    println("     Keys: ${index.indexFields}")
                    println("     Unique: ${index.isUnique}")
                    println("     Sparse: ${index.isSparse}")
                }
            }
            println("----------------------------------------")
        }
        println("========================================")
    }


}