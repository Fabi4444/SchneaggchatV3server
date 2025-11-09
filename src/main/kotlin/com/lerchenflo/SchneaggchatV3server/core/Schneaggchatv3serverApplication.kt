package com.lerchenflo.schneaggchatv3server.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication
@ComponentScan(basePackages = ["com.lerchenflo.schneaggchatv3server"])
@EnableMongoRepositories(basePackages = ["com.lerchenflo.schneaggchatv3server.repository"])
class Schneaggchatv3serverApplication

fun main(args: Array<String>) {
	runApplication<Schneaggchatv3serverApplication>(*args)
}


@EventListener(ApplicationReadyEvent::class)
fun onStartup() {
    //Code to execute on app start finished
}