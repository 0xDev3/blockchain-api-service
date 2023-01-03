package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.UtcDateTime

data class ApiUsagePeriod(
    val userId: UserId,
    val writeRequestUsage: RequestUsage,
    val readRequestUsage: RequestUsage,
    val startDate: UtcDateTime,
    val endDate: UtcDateTime
)

data class RequestUsage(val used: Long, val remaining: Long)
