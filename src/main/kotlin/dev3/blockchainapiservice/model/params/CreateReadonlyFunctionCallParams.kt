package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.request.ReadonlyFunctionCallRequest
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.WalletAddress

data class CreateReadonlyFunctionCallParams(
    val identifier: DeployedContractIdentifier,
    val blockNumber: BlockNumber?,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val outputParams: List<OutputParameter>,
    val callerAddress: WalletAddress
) {
    constructor(requestBody: ReadonlyFunctionCallRequest) : this(
        identifier = DeployedContractIdentifier(requestBody),
        blockNumber = requestBody.blockNumber?.let { BlockNumber(it) },
        functionName = requestBody.functionName,
        functionParams = requestBody.functionParams,
        outputParams = requestBody.outputParams,
        callerAddress = WalletAddress(requestBody.callerAddress)
    )
}
