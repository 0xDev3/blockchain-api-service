package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.util.UtcDateTime
import io.micrometer.core.instrument.util.NamedThreadFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface UuidProvider {
    fun getUuid(): UUID
}

@Service
class RandomUuidProvider : UuidProvider {
    override fun getUuid(): UUID = UUID.randomUUID()
}

interface UtcDateTimeProvider {
    fun getUtcDateTime(): UtcDateTime
}

@Service
class CurrentUtcDateTimeProvider : UtcDateTimeProvider {
    override fun getUtcDateTime(): UtcDateTime = UtcDateTime(OffsetDateTime.now())
}

interface RandomProvider {
    fun getBytes(length: Int): ByteArray
}

@Service
class SecureRandomProvider : RandomProvider {

    private val secureRandom = SecureRandom()

    override fun getBytes(length: Int): ByteArray {
        return ByteArray(length).apply { secureRandom.nextBytes(this) }
    }
}

interface FixedScheduler {
    fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit)
    fun shutdown()
}

interface ScheduledExecutorServiceProvider {
    fun newSingleThreadScheduledExecutor(threadPrefix: String): FixedScheduler
}

@Service
class DefaultScheduledExecutorServiceProvider : ScheduledExecutorServiceProvider {
    override fun newSingleThreadScheduledExecutor(threadPrefix: String): FixedScheduler =
        object : FixedScheduler {
            private val executor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(threadPrefix))

            override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit) {
                executor.scheduleAtFixedRate(command, initialDelay, period, unit)
            }

            override fun shutdown() = executor.shutdown()
        }
}
