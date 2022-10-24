package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.config.validation.ValidUint256
import java.math.BigInteger
import javax.validation.constraints.NotNull

data class MultiPaymentTemplateItemRequest(
    @field:NotNull
    @field:ValidEthAddress
    val walletAddress: String,
    @field:MaxStringSize
    val itemName: String?,
    @field:NotNull
    @field:ValidUint256
    val amount: BigInteger
)
