package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateMultiPaymentTemplateRequest(
    @field:NotNull
    @field:MaxStringSize
    val templateName: String,
    @field:NotNull
    val chainId: Long,
    @field:NotNull
    @field:Valid
    val items: List<MultiPaymentTemplateItemRequest>
)
