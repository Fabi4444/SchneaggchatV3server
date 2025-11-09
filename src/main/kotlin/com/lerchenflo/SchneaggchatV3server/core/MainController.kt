package com.lerchenflo.schneaggchatv3server.core

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * MainController for Ping etc.
 */

@RestController
class MainController {

    @GetMapping
    fun index(): String {
        return "Hello World!"
    }
}