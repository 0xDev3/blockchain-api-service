package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateContractDeploymentRequestParams(
    val contractId: ContractId,
    val constructorParams: List<FunctionArgument<*, *>>,
    val deployerAddress: WalletAddress?,
    val initialEthAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
)
