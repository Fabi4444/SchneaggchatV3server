@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.core

import com.lerchenflo.schneaggchatv3server.core.security.HashEncoder
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
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
    @Value($$"${defaultaccount.password}") private val defaultPassword: String
){

    @GetMapping
    fun index(): String {
        return "Hello World!"
    }

    @GetMapping("/test")
    fun test(): String {
        return "Up and running!"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {

        //Code to execute on app start finished

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
                    firebaseTokens = emptyList(),
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
            )
        }


    }


}