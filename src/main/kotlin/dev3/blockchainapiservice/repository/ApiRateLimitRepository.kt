package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.result.ApiUsageLimit
import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.util.UtcDateTime
import org.springframework.web.bind.annotation.RequestMethod
import java.util.UUID

interface ApiRateLimitRepository {
    fun createNewFutureUsageLimits(userId: UUID, currentTime: UtcDateTime, limits: List<ApiUsageLimit>)
    fun getCurrentApiUsagePeriod(userId: UUID, currentTime: UtcDateTime): ApiUsagePeriod
    fun remainingWriteLimit(userId: UUID, currentTime: UtcDateTime): Long
    fun remainingReadLimit(userId: UUID, currentTime: UtcDateTime): Long
    fun addWriteCall(userId: UUID, currentTime: UtcDateTime, method: RequestMethod, endpoint: String)
    fun addReadCall(userId: UUID, currentTime: UtcDateTime, endpoint: String)
}
