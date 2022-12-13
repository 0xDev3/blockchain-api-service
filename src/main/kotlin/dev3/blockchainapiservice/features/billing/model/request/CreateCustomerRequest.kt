package dev3.blockchainapiservice.features.billing.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import javax.validation.constraints.NotNull

data class CreateCustomerRequest(
    @field:NotNull
    @field:MaxStringSize
    val email: String
)
