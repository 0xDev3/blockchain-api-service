package com.ampnet.blockchainapiservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BlockchainApiServiceApplication

fun main(vararg args: String) {
    runApplication<BlockchainApiServiceApplication>(*args)
}
