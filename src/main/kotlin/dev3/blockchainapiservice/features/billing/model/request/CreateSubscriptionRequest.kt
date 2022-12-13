package dev3.blockchainapiservice.features.billing.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import java.util.UUID
import javax.validation.constraints.NotNull

data class CreateSubscriptionRequest(
    @field:NotNull
    val projectId: UUID,
    @field:NotNull
    @field:MaxStringSize
    val priceId: String
)
