package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.CreateAuthorizationRequest
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateAuthorizationRequestParams(
    val requestedWalletAddress: WalletAddress?,
    val redirectUrl: String?,
    val messageToSign: String?,
    val storeIndefinitely: Boolean,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateAuthorizationRequest) : this(
        requestedWalletAddress = requestBody.walletAddress?.let { WalletAddress(it) },
        redirectUrl = requestBody.redirectUrl,
        messageToSign = requestBody.messageToSign,
        storeIndefinitely = requestBody.storeIndefinitely ?: true,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
