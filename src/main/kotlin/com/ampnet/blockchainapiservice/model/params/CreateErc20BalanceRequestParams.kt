package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.CreateErc20BalanceRequest
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateErc20BalanceRequestParams(
    override val clientId: String?,
    val chainId: ChainId?,
    val redirectUrl: String?,
    val tokenAddress: ContractAddress?,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) : ClientIdParam {
    constructor(requestBody: CreateErc20BalanceRequest) : this(
        clientId = requestBody.clientId,
        chainId = requestBody.chainId?.let { ChainId(it) },
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
        blockNumber = requestBody.blockNumber?.let { BlockNumber(it) },
        requestedWalletAddress = requestBody.walletAddress?.let { WalletAddress(it) },
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
