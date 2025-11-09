package com.lerchenflo.SchneaggchatV3server.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.lerchenflo.schneaggchatv3server"])


class SchneaggchatV3serverApplication

fun main(args: Array<String>) {
	runApplication<SchneaggchatV3serverApplication>(*args)
}
