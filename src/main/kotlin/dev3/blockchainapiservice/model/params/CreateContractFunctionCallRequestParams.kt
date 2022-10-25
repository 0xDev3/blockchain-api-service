package dev3.blockchainapiservice.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.request.CreateContractFunctionCallRequest
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.WalletAddress

data class CreateContractFunctionCallRequestParams(
    val identifier: DeployedContractIdentifier,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val ethAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    constructor(requestBody: CreateContractFunctionCallRequest) : this(
        identifier = DeployedContractIdentifier(requestBody),
        functionName = requestBody.functionName,
        functionParams = requestBody.functionParams,
        ethAmount = Balance(requestBody.ethAmount),
        redirectUrl = requestBody.redirectUrl,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY,
        callerAddress = requestBody.callerAddress?.let { WalletAddress(it) }
    )
}
