package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import javax.validation.constraints.NotNull

data class AttachSignedMessageRequest(
    @field:NotNull
    @field:ValidEthAddress
    val walletAddress: String,
    @field:NotNull
    @field:MaxStringSize
    val signedMessage: String
)
