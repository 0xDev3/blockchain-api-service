package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class StoreErc20LockRequestParams(
    val id: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val lockContractAddress: ContractAddress,
    val tokenSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    companion object : ParamsFactory<CreateErc20LockRequestParams, StoreErc20LockRequestParams> {
        override fun fromCreateParams(id: UUID, params: CreateErc20LockRequestParams, clientInfo: ClientInfo) =
            StoreErc20LockRequestParams(
                id = id,
                chainId = clientInfo.chainId.resolve(params.chainId),
                redirectUrl = clientInfo.sendRedirectUrl.resolve(params.redirectUrl).replace("\${id}", id.toString()),
                tokenAddress = clientInfo.tokenAddress.resolve(params.tokenAddress),
                tokenAmount = params.tokenAmount,
                tokenSenderAddress = params.tokenSenderAddress,
                lockContractAddress = params.lockContractAddress,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
    }
}
