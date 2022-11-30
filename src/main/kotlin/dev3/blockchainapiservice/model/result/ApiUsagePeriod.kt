package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.util.UtcDateTime
import java.util.UUID

data class ApiUsagePeriod(
    val projectId: UUID,
    val writeRequestUsage: RequestUsage,
    val readRequestUsage: RequestUsage,
    val startDate: UtcDateTime,
    val endDate: UtcDateTime
)

data class RequestUsage(val used: Int, val remaining: Int)