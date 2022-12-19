package dev3.blockchainapiservice.features.billing.model.response

import java.math.BigDecimal

data class SubscriptionPriceResponse(
    val id: String,
    val currency: String,
    val amount: BigDecimal,
    val intervalType: IntervalType,
    val intervalDuration: Long
)

enum class IntervalType(private val months: Int) {
    DAY(months = 0), WEEK(months = 0), MONTH(months = 1), YEAR(months = 12);

    fun toMonths(intervalLength: Long): Long = intervalLength * months
}
