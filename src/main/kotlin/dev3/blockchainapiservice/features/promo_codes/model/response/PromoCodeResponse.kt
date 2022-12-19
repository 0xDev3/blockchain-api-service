package dev3.blockchainapiservice.features.promo_codes.model.response

import java.math.BigInteger
import java.time.OffsetDateTime

data class PromoCodeResponse(
    val code: String,
    val writeRequests: BigInteger,
    val readRequests: BigInteger,
    val numOfUsages: BigInteger,
    val validUntil: OffsetDateTime
)
