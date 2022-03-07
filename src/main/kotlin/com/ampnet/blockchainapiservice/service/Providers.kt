package com.ampnet.blockchainapiservice.service

import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

interface UuidProvider {
    fun getUuid(): UUID
}

@Service
class RandomUuidProvider : UuidProvider {
    override fun getUuid(): UUID = UUID.randomUUID()
}

interface OffsetDateTimeProvider {
    fun getOffsetDateTime(): OffsetDateTime
}

@Service
class CurrentOffsetDateTimeProvider : OffsetDateTimeProvider {
    override fun getOffsetDateTime(): OffsetDateTime = OffsetDateTime.now()
}
