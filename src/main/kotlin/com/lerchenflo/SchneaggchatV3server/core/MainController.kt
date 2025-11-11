@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.core

import com.lerchenflo.schneaggchatv3server.core.security.JwtService
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.model.User
import org.bson.types.ObjectId
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * MainController for Ping etc.
 */

@RestController
class MainController(
    private val userRepository: UserRepository,
){

    @GetMapping
    fun index(): String {
        return "Hello World!"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {


        //Code to execute on app start finished
        userRepository.save(
            User(
                id = ObjectId.get(),
                username = "john doe",
                hashedPassword = "",
                email = "awda",
                profilePictureUrl = "",
                userDescription = "",
                userStatus = "aw",
                firebaseTokens = emptyList(),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                birthDate = "adawdawd",
            )
        )

    }


}