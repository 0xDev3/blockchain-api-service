package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.result.ApiUsage
import dev3.blockchainapiservice.util.UtcDateTime
import org.springframework.web.bind.annotation.RequestMethod
import java.util.UUID

interface ApiRateLimitRepository {
    fun getCurrentApiUsage(userId: UUID): ApiUsage
    fun usedWriteRequests(userId: UUID): Long
    fun usedReadRequests(userId: UUID): Long
    fun addWriteCall(userId: UUID, currentTime: UtcDateTime, method: RequestMethod, endpoint: String)
    fun addReadCall(userId: UUID, currentTime: UtcDateTime, endpoint: String)
}
