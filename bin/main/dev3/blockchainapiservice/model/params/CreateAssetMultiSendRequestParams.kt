package dev3.blockchainapiservice.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.request.CreateAssetMultiSendRequest
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress

data class CreateAssetMultiSendRequestParams(
    val redirectUrl: String?,
    val tokenAddress: ContractAddress?,
    val disperseContractAddress: ContractAddress,
    val assetAmounts: List<Balance>,
    val assetRecipientAddresses: List<WalletAddress>,
    val itemNames: List<String?>,
    val assetSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val approveScreenConfig: ScreenConfig,
    val disperseScreenConfig: ScreenConfig
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
        approveScreenConfig = requestBody.approveScreenConfig ?: ScreenConfig.EMPTY,
        disperseScreenConfig = requestBody.disperseScreenConfig ?: ScreenConfig.EMPTY
    )
}
