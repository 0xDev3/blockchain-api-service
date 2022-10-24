package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.fasterxml.jackson.databind.JsonNode
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
