package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.CreateErc20LockRequest
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.DurationSeconds
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateErc20LockRequestParams(
    override val clientId: String?,
    val chainId: ChainId?,
    val redirectUrl: String?,
    val tokenAddress: ContractAddress?,
    val tokenAmount: Balance,
    val lockDuration: DurationSeconds,
    val lockContractAddress: ContractAddress,
    val tokenSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) : ClientIdParam {
    constructor(requestBody: CreateErc20LockRequest) : this(
        clientId = requestBody.clientId,
        chainId = requestBody.chainId?.let { ChainId(it) },
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
        tokenAmount = Balance(requestBody.amount),
        lockDuration = DurationSeconds(requestBody.lockDurationInSeconds),
        lockContractAddress = ContractAddress(requestBody.lockContractAddress),
        tokenSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
