package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.util.UtcDateTime
import org.springframework.web.bind.annotation.RequestMethod
import java.util.UUID

interface ApiRateLimitRepository {
    fun getCurrentApiUsagePeriod(projectId: UUID, currentTime: UtcDateTime): ApiUsagePeriod
    fun remainingWriteLimit(projectId: UUID, currentTime: UtcDateTime): Int
    fun remainingReadLimit(projectId: UUID, currentTime: UtcDateTime): Int
    fun addWriteCall(projectId: UUID, currentTime: UtcDateTime, method: RequestMethod, endpoint: String)
    fun addReadCall(projectId: UUID, currentTime: UtcDateTime, endpoint: String)
}
