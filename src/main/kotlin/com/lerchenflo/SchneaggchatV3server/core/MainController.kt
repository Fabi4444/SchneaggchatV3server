@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.core

import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.ExperimentalTime

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


    }


}