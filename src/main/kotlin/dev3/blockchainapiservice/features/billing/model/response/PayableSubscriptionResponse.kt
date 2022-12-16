package dev3.blockchainapiservice.features.billing.model.response

import com.fasterxml.jackson.databind.JsonNode
import java.time.OffsetDateTime

data class PayableSubscriptionResponse(
    val id: String,
    val isActive: Boolean,
    val stripePublishableKey: String,
    val currentPeriodStart: OffsetDateTime,
    val currentPeriodEnd: OffsetDateTime,
    val stripeSubscriptionData: JsonNode
)
