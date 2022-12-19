package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.util.UtcDateTime
import java.util.UUID

data class ApiUsageLimit(
    val userId: UUID,
    val allowedWriteRequests: Long,
    val allowedReadRequests: Long,
    val startDate: UtcDateTime,
    val endDate: UtcDateTime
)
