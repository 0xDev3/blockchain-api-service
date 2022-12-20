package dev3.blockchainapiservice.features.promocodes.repository

import dev3.blockchainapiservice.features.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.promocodes.model.result.PromoCodeResult
import dev3.blockchainapiservice.util.UtcDateTime
import java.util.UUID

interface PromoCodeRepository {
    fun storeCode(code: String, writeRequests: Long, readRequests: Long, validUntil: UtcDateTime): PromoCode
    fun getCodes(validFrom: UtcDateTime, validUntil: UtcDateTime): List<PromoCode>
    fun useCode(code: String, userId: UUID, currentTime: UtcDateTime): PromoCodeResult
}
