package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.result.ApiUsageLimit
import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.util.UtcDateTime
import org.springframework.web.bind.annotation.RequestMethod

interface ApiRateLimitRepository {
    fun createNewFutureUsageLimits(userId: UserId, currentTime: UtcDateTime, limits: List<ApiUsageLimit>)
    fun getCurrentApiUsagePeriod(userId: UserId, currentTime: UtcDateTime): ApiUsagePeriod
    fun remainingWriteLimit(userId: UserId, currentTime: UtcDateTime): Long
    fun remainingReadLimit(userId: UserId, currentTime: UtcDateTime): Long
    fun addWriteCall(userId: UserId, currentTime: UtcDateTime, method: RequestMethod, endpoint: String)
    fun addReadCall(userId: UserId, currentTime: UtcDateTime, endpoint: String)
}
