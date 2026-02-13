@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.core

import com.lerchenflo.schneaggchatv3server.core.security.HashEncoder
import com.lerchenflo.schneaggchatv3server.group.GroupLookupService
import com.lerchenflo.schneaggchatv3server.group.GroupService
import com.lerchenflo.schneaggchatv3server.repository.GroupRepository
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.UserLookupService
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate
import org.springframework.data.mongodb.core.aggregation.SetOperation
import org.springframework.data.mongodb.core.index.IndexInfo
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * MainController for Ping etc.
 */

@RestController
class MainController(
    private val userLookupService: UserLookupService,
    private val groupLookupService: GroupLookupService,

    private val hashEncoder: HashEncoder,
    private val mongoTemplate: MongoTemplate,
    private val groupService: GroupService,
    private val groupRepository: GroupRepository,

    private val userRepository: UserRepository,


    @Value("\${defaultaccount.password}") private val defaultPassword: String
){

    @GetMapping("/public/test")
    fun test(): String {
        return "Up and running!"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {

        migrateDBs()

        //Code to execute on app start finished
        //listMongoIndexes()
        printAllGroups()

        //Create default Account for Google play / App Store
        val defaultUserUserName = "testaccount"
        val defaultUser = userLookupService.findByUsername(defaultUserUserName)
        if(defaultUser == null){
            userLookupService.save(
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


    fun migrateDBs() {
        println("Running database migrations...")

        val query = Query()
        query.addCriteria(Criteria.where("profilePicUpdatedAt").exists(false))

        // Use SetOperation to reference the updatedAt field
        val update = AggregationUpdate.update()
            .set(
                SetOperation.builder()
                .set("profilePicUpdatedAt")
                .toValueOf("updatedAt")
            )

        val result = mongoTemplate.updateMulti(
            query,
            update,
            User::class.java
        )

        if (result.modifiedCount > 0) {
            println("✅ Migration completed: Added profilePicUpdatedAt field to ${result.modifiedCount} users")
        } else {
            println("✅ Migration check: All users already have profilePicUpdatedAt field")
        }
    }



    /**
     * Prints all groups with their members, creator, and admin status in a table format.
     * ★ = Creator, ☆ = Admin (non-creator)
     */
    fun printAllGroups() {
        val groups = groupRepository.findAll()

        if (groups.isEmpty()) {
            println("No groups found.")
            return
        }

        println("\n╔════════════════════════════════════════════════════════════════════════════════╗")
        println("║                              ALL GROUPS REPORT                                 ║")
        println("╠════════════════════════════════════════════════════════════════════════════════╣")

        groups.forEach { group ->
            val members = groupLookupService.getGroupMembers(group.id)
            val creatorId = group.creatorId

            println("║                                                                                ║")
            println("║  Group: ${group.name.padEnd(68)}║")
            println("║  Description: ${group.description.take(60).padEnd(62)}║")
            println("║  ID: ${group.id.toHexString().padEnd(71)}║")
            println("╟────────────────────────────────────────────────────────────────────────────────╢")
            println("║  Members:                                                                      ║")
            println("║  ┌──────────────────────────────────┬──────────────┬──────────────────────────┐║")
            println("║  │ Username                         │ Role         │ User ID                  │║")
            println("║  ├──────────────────────────────────┼──────────────┼──────────────────────────┤║")

            members.forEach { member ->
                val username = userLookupService.getUsername(member.userid)
                val isCreator = member.userid == creatorId
                val isAdmin = member.admin

                val roleMarker = when {
                    isCreator -> "★ Creator"
                    isAdmin -> "☆ Admin"
                    else -> "  Member"
                }

                val displayName = username.take(30).padEnd(32)
                val roleDisplay = roleMarker.padEnd(12)
                val userIdShort = member.userid.toHexString().take(24).padEnd(24)

                println("║  │ $displayName │ $roleDisplay │ $userIdShort │║")
            }

            println("║  └──────────────────────────────────┴──────────────┴──────────────────────────┘║")
            println("║  Total members: ${members.size.toString().padEnd(60)}║")
            println("╠════════════════════════════════════════════════════════════════════════════════╣")
        }

        println("║  Total groups: ${groups.size.toString().padEnd(61)}║")
        println("╚════════════════════════════════════════════════════════════════════════════════╝\n")
    }


}