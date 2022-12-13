package dev3.blockchainapiservice.features.billing.model.response

import java.math.BigDecimal

data class SubscriptionPriceResponse(
    val id: String,
    val currency: String,
    val amount: BigDecimal,
    val intervalType: IntervalType,
    val intervalDuration: Long
)

enum class IntervalType {
    DAY, WEEK, MONTH, YEAR
}
