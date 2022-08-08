package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.config.validation.ValidEthTxHash
import javax.validation.constraints.NotNull

data class AttachTransactionInfoRequest(
    @field:NotNull
    @field:ValidEthTxHash
    val txHash: String,
    @field:NotNull
    @field:ValidEthAddress
    val callerAddress: String
)
