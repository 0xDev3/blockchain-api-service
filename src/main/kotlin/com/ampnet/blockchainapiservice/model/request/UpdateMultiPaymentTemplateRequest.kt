package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import javax.validation.constraints.NotNull

data class UpdateMultiPaymentTemplateRequest(
    @field:NotNull
    @field:MaxStringSize
    val templateName: String,
    @field:NotNull
    val chainId: Long
)
