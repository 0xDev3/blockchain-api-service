package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class StoreSendErc20RequestParams(
    val id: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val tokenSenderAddress: WalletAddress?,
    val tokenRecipientAddress: WalletAddress,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    companion object {
        fun fromCreateParams(params: CreateSendErc20RequestParams, id: UUID, chainId: ChainId, redirectUrl: String) =
            StoreSendErc20RequestParams(
                id = id,
                chainId = chainId,
                redirectUrl = redirectUrl.replace("\${id}", id.toString()),
                tokenAddress = params.tokenAddress,
                tokenAmount = params.tokenAmount,
                tokenSenderAddress = params.tokenSenderAddress,
                tokenRecipientAddress = params.tokenRecipientAddress,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
    }
}
