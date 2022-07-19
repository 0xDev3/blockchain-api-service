package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.CreateContractDeploymentRequest
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateContractDeploymentRequestParams(
    val alias: String,
    val contractId: ContractId,
    val constructorParams: List<FunctionArgument>,
    val deployerAddress: WalletAddress?,
    val initialEthAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateContractDeploymentRequest) : this(
        alias = requestBody.alias,
        contractId = ContractId(requestBody.contractId),
        constructorParams = requestBody.constructorParams,
        deployerAddress = requestBody.deployerAddress?.let { WalletAddress(it) },
        initialEthAmount = Balance(requestBody.initialEthAmount),
        redirectUrl = requestBody.redirectUrl,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig
    )
}
