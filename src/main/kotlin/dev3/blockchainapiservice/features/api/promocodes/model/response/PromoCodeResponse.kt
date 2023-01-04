package dev3.blockchainapiservice.features.api.promocodes.model.response

import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCode
import java.math.BigInteger
import java.time.OffsetDateTime

data class PromoCodeResponse(
    val code: String,
    val writeRequests: BigInteger,
    val readRequests: BigInteger,
    val numOfUsages: BigInteger,
    val validUntil: OffsetDateTime
) {
    constructor(promoCode: PromoCode) : this(
        code = promoCode.code,
        writeRequests = promoCode.writeRequests.toBigInteger(),
        readRequests = promoCode.readRequests.toBigInteger(),
        numOfUsages = promoCode.numOfUsages.toBigInteger(),
        validUntil = promoCode.validUntil.value
    )
}
