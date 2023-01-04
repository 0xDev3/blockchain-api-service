package dev3.blockchainapiservice.features.wallet.authorization.model.request

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.validation.MaxJsonNodeChars
import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.model.ScreenConfig
import javax.validation.Valid

data class CreateAuthorizationRequest(
    @field:ValidEthAddress
    val walletAddress: String?,
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:MaxStringSize
    val messageToSign: String?,
    val storeIndefinitely: Boolean?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
)
