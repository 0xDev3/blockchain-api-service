package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class CreateContractFunctionCallRequestParams(
    val identifier: DeployedContractIdentifier,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val ethAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
)

sealed interface DeployedContractIdentifier

data class DeployedContractIdIdentifier(val id: UUID) : DeployedContractIdentifier
data class DeployedContractAliasIdentifier(val alias: String) : DeployedContractIdentifier
data class DeployedContractAddressIdentifier(val contractAddress: ContractAddress) : DeployedContractIdentifier
