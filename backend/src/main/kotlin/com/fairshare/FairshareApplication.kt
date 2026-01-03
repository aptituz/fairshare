package com.fairshare

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FairshareApplication

fun main(args: Array<String>) {
    runApplication<FairshareApplication>(*args)
}
