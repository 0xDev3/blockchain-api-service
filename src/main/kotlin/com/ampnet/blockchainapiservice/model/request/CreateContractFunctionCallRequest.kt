package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger
import java.util.UUID

data class CreateContractFunctionCallRequest(
    val deployedContractId: UUID?,
    val deployedContractAlias: String?,
    val contractAddress: String?,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val ethAmount: BigInteger,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val callerAddress: String?
)
