package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.CreateErc20SendRequest
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateErc20SendRequestParams(
    val redirectUrl: String?,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val tokenSenderAddress: WalletAddress?,
    val tokenRecipientAddress: WalletAddress,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateErc20SendRequest) : this(
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = ContractAddress(requestBody.tokenAddress),
        tokenAmount = Balance(requestBody.amount),
        tokenSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
        tokenRecipientAddress = WalletAddress(requestBody.recipientAddress),
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
