package dev3.blockchainapiservice.features.promo_codes.repository

import dev3.blockchainapiservice.features.promo_codes.model.result.PromoCode
import dev3.blockchainapiservice.util.UtcDateTime

interface PromoCodeRepository {
    fun storeCode(code: String, writeRequests: Long, readRequests: Long, validUntil: UtcDateTime): PromoCode
    fun getCodes(validFrom: UtcDateTime, validUntil: UtcDateTime): List<PromoCode>
    fun useCode(code: String, currentTime: UtcDateTime): PromoCode?
}
