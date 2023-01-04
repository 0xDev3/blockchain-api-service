package dev3.blockchainapiservice.features.asset.multisend.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.config.validation.ValidUint256
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
