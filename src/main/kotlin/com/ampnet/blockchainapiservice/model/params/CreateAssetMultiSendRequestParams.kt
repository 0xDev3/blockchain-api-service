package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.CreateAssetMultiSendRequest
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateAssetMultiSendRequestParams(
    val redirectUrl: String?,
    val tokenAddress: ContractAddress?,
    val disperseContractAddress: ContractAddress,
    val assetAmounts: List<Balance>,
    val assetRecipientAddresses: List<WalletAddress>,
    val itemNames: List<String?>,
    val assetSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val sendScreenConfig: ScreenConfig,
    val approveScreenConfig: ScreenConfig
) {
    constructor(requestBody: CreateAssetMultiSendRequest) : this(
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
        disperseContractAddress = ContractAddress(requestBody.disperseContractAddress),
        assetAmounts = requestBody.items.map { Balance(it.amount) },
        assetRecipientAddresses = requestBody.items.map { WalletAddress(it.walletAddress) },
        itemNames = requestBody.items.map { it.itemName },
        assetSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
        arbitraryData = requestBody.arbitraryData,
        sendScreenConfig = requestBody.sendScreenConfig ?: ScreenConfig.EMPTY,
        approveScreenConfig = requestBody.approveScreenConfig ?: ScreenConfig.EMPTY
    )
}
