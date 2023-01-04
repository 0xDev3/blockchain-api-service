package dev3.blockchainapiservice.features.api.billing.model.response

import java.math.BigInteger

data class AvailableSubscriptionResponse(
    val id: String,
    val name: String,
    val description: String,
    val readRequests: BigInteger,
    val writeRequests: BigInteger,
    val prices: List<SubscriptionPriceResponse>
)
