package com.jumpbeat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JumpBeatApplication

fun main(args: Array<String>) {
	runApplication<JumpBeatApplication>(*args)
}
