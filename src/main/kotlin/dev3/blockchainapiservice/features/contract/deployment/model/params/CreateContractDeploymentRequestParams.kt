package dev3.blockchainapiservice.features.contract.deployment.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.features.contract.deployment.model.request.CreateContractDeploymentRequest
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.WalletAddress

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
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
