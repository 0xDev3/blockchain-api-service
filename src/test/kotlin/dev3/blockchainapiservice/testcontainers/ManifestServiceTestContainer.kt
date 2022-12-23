package dev3.blockchainapiservice.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class ManifestServiceTestContainer : GenericContainer<ManifestServiceTestContainer>(
    "ampnet/contracts-manifest-service:0.6.0"
) {

    @Suppress("unused")
    companion object {
        private const val SERVICE_PORT = 42070
    }

    init {
        waitStrategy = LogMessageWaitStrategy()
            .withRegEx("Example app listening at .*")
            .withTimes(1)
            .withStartupTimeout(60.seconds.toJavaDuration())

        addExposedPort(SERVICE_PORT)
        start()

        val mappedPort = getMappedPort(SERVICE_PORT).toString()

        System.setProperty("MANIFEST_SERVICE_PORT", mappedPort)
    }
}
