package dev3.blockchainapiservice.features.api.billing.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import javax.validation.constraints.NotNull

data class CreateSubscriptionRequest(
    @field:NotNull
    @field:MaxStringSize
    val priceId: String
)
