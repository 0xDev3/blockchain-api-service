package dev3.blockchainapiservice.features.promo_codes.model.result

import dev3.blockchainapiservice.util.UtcDateTime

data class PromoCode(
    val code: String,
    val writeRequests: Long,
    val readRequests: Long,
    val numOfUsages: Long,
    val validUntil: UtcDateTime
)
