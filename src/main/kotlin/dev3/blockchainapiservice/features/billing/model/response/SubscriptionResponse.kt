package dev3.blockchainapiservice.features.billing.model.response

import com.fasterxml.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class SubscriptionResponse(
    val id: String,
    val projectId: UUID,
    val currentPeriodStart: OffsetDateTime,
    val currentPeriodEnd: OffsetDateTime,
    val stripeSubscriptionData: JsonNode
)
