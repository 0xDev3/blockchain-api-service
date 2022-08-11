package com.ampnet.blockchainapiservice.testcontainers

object SharedTestContainers {
    val postgresContainer = PostgresTestContainer()
    val hardhatContainer = HardhatTestContainer()
}
