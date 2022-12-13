package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.util.UtcDateTime
import org.springframework.web.bind.annotation.RequestMethod
import java.util.UUID

interface ApiRateLimitRepository {
    fun getCurrentApiUsagePeriod(userId: UUID, currentTime: UtcDateTime): ApiUsagePeriod
    fun remainingWriteLimit(userId: UUID, currentTime: UtcDateTime): Int
    fun remainingReadLimit(userId: UUID, currentTime: UtcDateTime): Int
    fun addWriteCall(userId: UUID, currentTime: UtcDateTime, method: RequestMethod, endpoint: String)
    fun addReadCall(userId: UUID, currentTime: UtcDateTime, endpoint: String)
}
