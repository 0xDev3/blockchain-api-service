package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger

data class CreateContractDeploymentRequest(
    val contractId: String,
    val constructorParams: List<FunctionArgument>,
    val deployerAddress: String?,
    val initialEthAmount: BigInteger,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
)
