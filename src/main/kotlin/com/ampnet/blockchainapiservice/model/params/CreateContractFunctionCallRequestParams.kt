package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.CreateContractFunctionCallRequest
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

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
