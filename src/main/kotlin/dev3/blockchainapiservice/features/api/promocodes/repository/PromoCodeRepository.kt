package dev3.blockchainapiservice.features.api.promocodes.repository

import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeResult
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.UtcDateTime

interface PromoCodeRepository {
    fun storeCode(code: String, writeRequests: Long, readRequests: Long, validUntil: UtcDateTime): PromoCode
    fun getCodes(validFrom: UtcDateTime, validUntil: UtcDateTime): List<PromoCode>
    fun useCode(code: String, userId: UserId, currentTime: UtcDateTime): PromoCodeResult
}
