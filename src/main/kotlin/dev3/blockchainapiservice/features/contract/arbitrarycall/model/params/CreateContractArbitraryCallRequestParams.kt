package dev3.blockchainapiservice.features.contract.arbitrarycall.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.request.CreateContractArbitraryCallRequest
import dev3.blockchainapiservice.features.contract.deployment.model.params.DeployedContractIdentifier
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.WalletAddress

data class CreateContractArbitraryCallRequestParams(
    val identifier: DeployedContractIdentifier,
    val functionData: FunctionData,
    val ethAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    constructor(requestBody: CreateContractArbitraryCallRequest) : this(
        identifier = DeployedContractIdentifier(requestBody),
        functionData = FunctionData(requestBody.functionData),
        ethAmount = Balance(requestBody.ethAmount),
        redirectUrl = requestBody.redirectUrl,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY,
        callerAddress = requestBody.callerAddress?.let { WalletAddress(it) }
    )
}
