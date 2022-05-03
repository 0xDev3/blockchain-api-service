package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class StoreErc20BalanceRequestParams(
    val id: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    companion object {
        fun fromCreateParams(id: UUID, params: CreateErc20BalanceRequestParams, clientInfo: ClientInfo) =
            StoreErc20BalanceRequestParams(
                id = id,
                chainId = clientInfo.chainId.resolve(params.chainId),
                redirectUrl = clientInfo.balanceRedirectUrl.resolve(params.redirectUrl)
                    .replace("\${id}", id.toString()),
                tokenAddress = clientInfo.tokenAddress.resolve(params.tokenAddress),
                blockNumber = params.blockNumber,
                requestedWalletAddress = params.requestedWalletAddress,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
    }
}
