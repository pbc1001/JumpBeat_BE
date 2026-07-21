package com.jumpbeat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class JumpBeatApplication

fun main(args: Array<String>) {
	runApplication<JumpBeatApplication>(*args)
}
