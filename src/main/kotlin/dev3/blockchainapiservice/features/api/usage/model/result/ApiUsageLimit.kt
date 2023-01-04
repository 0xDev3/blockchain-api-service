package dev3.blockchainapiservice.features.api.usage.model.result

import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.UtcDateTime

data class ApiUsageLimit(
    val userId: UserId,
    val allowedWriteRequests: Long,
    val allowedReadRequests: Long,
    val startDate: UtcDateTime,
    val endDate: UtcDateTime
)
