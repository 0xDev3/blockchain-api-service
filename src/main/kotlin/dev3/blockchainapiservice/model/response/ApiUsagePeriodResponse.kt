package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.model.result.RequestUsage
import java.time.OffsetDateTime

data class ApiUsagePeriodResponse(
    val userId: UserId,
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
