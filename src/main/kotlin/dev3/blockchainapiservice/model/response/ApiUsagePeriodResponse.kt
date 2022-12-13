package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.model.result.RequestUsage
import java.time.OffsetDateTime
import java.util.UUID

data class ApiUsagePeriodResponse(
    val userId: UUID,
    val writeRequestUsage: RequestUsage,
    val readRequestUsage: RequestUsage,
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime
) {
    constructor(apiUsagePeriod: ApiUsagePeriod) : this(
        userId = apiUsagePeriod.userId,
        writeRequestUsage = apiUsagePeriod.writeRequestUsage,
        readRequestUsage = apiUsagePeriod.readRequestUsage,
        startDate = apiUsagePeriod.startDate.value,
        endDate = apiUsagePeriod.endDate.value
    )
}
