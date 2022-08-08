package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import javax.validation.constraints.NotNull

data class AttachSignedMessageRequest(
    @field:NotNull
    @field:ValidEthAddress
    val walletAddress: String,
    @field:NotNull
    @field:MaxStringSize
    val signedMessage: String
)
