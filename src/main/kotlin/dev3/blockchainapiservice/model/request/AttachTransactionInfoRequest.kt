package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.config.validation.ValidEthTxHash
import javax.validation.constraints.NotNull

data class AttachTransactionInfoRequest(
    @field:NotNull
    @field:ValidEthTxHash
    val txHash: String,
    @field:NotNull
    @field:ValidEthAddress
    val callerAddress: String
)
