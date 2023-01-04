package dev3.blockchainapiservice.features.asset.send.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.features.asset.send.model.request.CreateAssetSendRequest
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress

data class CreateAssetSendRequestParams(
    val redirectUrl: String?,
    val tokenAddress: ContractAddress?,
    val assetAmount: Balance,
    val assetSenderAddress: WalletAddress?,
    val assetRecipientAddress: WalletAddress,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateAssetSendRequest) : this(
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
        assetAmount = Balance(requestBody.amount),
        assetSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
        assetRecipientAddress = WalletAddress(requestBody.recipientAddress),
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
