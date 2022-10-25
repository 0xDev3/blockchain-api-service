package dev3.blockchainapiservice.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.request.CreateErc20LockRequest
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.DurationSeconds
import dev3.blockchainapiservice.util.WalletAddress

data class CreateErc20LockRequestParams(
    val redirectUrl: String?,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val lockDuration: DurationSeconds,
    val lockContractAddress: ContractAddress,
    val tokenSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateErc20LockRequest) : this(
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = ContractAddress(requestBody.tokenAddress),
        tokenAmount = Balance(requestBody.amount),
        lockDuration = DurationSeconds(requestBody.lockDurationInSeconds),
        lockContractAddress = ContractAddress(requestBody.lockContractAddress),
        tokenSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
