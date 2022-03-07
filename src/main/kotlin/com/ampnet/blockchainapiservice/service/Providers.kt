package com.ampnet.blockchainapiservice.service

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

interface UuidProvider {
    fun getUuid(): UUID
}

@Service
class RandomUuidProvider : UuidProvider {
    override fun getUuid(): UUID = UUID.randomUUID()
}

interface LocalDateTimeProvider {
    fun getLocalDateTime(): LocalDateTime
}

@Service
class CurrentLocalDateTimeProvider : LocalDateTimeProvider {
    override fun getLocalDateTime(): LocalDateTime = LocalDateTime.now()
}
