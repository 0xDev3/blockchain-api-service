package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.UtcDateTime
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.UUID

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
