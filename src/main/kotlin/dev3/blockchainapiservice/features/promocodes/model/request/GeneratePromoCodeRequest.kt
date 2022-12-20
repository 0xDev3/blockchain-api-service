package dev3.blockchainapiservice.features.promocodes.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import javax.validation.constraints.PositiveOrZero

data class GeneratePromoCodeRequest(
    @field:MaxStringSize
    val prefix: String?,
    @field:PositiveOrZero
    val writeRequests: Long?,
    @field:PositiveOrZero
    val readRequests: Long?,
    @field:PositiveOrZero
    val validityInDays: Long?
)
