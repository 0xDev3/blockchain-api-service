package dev3.blockchainapiservice.features.promocodes.model.result

import dev3.blockchainapiservice.util.UtcDateTime

sealed interface PromoCodeResult

object PromoCodeExpired : PromoCodeResult
object PromoCodeDoesNotExist : PromoCodeResult
object PromoCodeAlreadyUsed : PromoCodeResult

data class PromoCode(
    val code: String,
    val writeRequests: Long,
    val readRequests: Long,
    val numOfUsages: Long,
    val validUntil: UtcDateTime
) : PromoCodeResult
