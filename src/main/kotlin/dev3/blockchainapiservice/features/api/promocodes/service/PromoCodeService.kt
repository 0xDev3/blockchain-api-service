package dev3.blockchainapiservice.features.api.promocodes.service

import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.util.UtcDateTime

interface PromoCodeService {
    fun generatePromoCode(
        userIdentifier: UserIdentifier,
        prefix: String,
        writeRequests: Long,
        readRequests: Long,
        validUntil: UtcDateTime
    ): PromoCode

    fun getPromoCodes(userIdentifier: UserIdentifier, validFrom: UtcDateTime, validUntil: UtcDateTime): List<PromoCode>
    fun usePromoCode(userIdentifier: UserIdentifier, code: String)
}
