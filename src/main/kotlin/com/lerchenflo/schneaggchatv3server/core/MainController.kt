@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.core

import com.lerchenflo.schneaggchatv3server.core.security.HashEncoder
import com.lerchenflo.schneaggchatv3server.group.GroupService
import com.lerchenflo.schneaggchatv3server.repository.GroupRepository
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.UserService
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
    private val groupService: GroupService,
    private val groupRepository: GroupRepository,
    @Value("\${defaultaccount.password}") private val defaultPassword: String
){

    @GetMapping("/public/test")
    fun test(): String {
        return "Up and running!"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        //Code to execute on app start finished
        //listMongoIndexes()
        printAllGroups()

        //Create default Account for Google play / App Store
        val defaultUserUserName = "testaccount"
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
            val members = groupService.getGroupMembers(group.id)
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
                val username = userService.getUsername(member.userid) ?: "Unknown"
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