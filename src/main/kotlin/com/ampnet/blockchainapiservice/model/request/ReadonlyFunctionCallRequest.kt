package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.model.params.DeployedContractIdentifierRequestBody
import com.ampnet.blockchainapiservice.util.FunctionArgument
import java.math.BigInteger
import java.util.UUID

data class ReadonlyFunctionCallRequest(
    override val deployedContractId: UUID?,
    override val deployedContractAlias: String?,
    override val contractAddress: String?,
    val blockNumber: BigInteger?,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val outputParameters: List<String>, // TODO use more specific type
    val callerAddress: String
) : DeployedContractIdentifierRequestBody
