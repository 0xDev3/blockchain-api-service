package dev3.blockchainapiservice.model.params

import dev3.blockchainapiservice.model.request.ReadonlyFunctionCallRequest
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.WalletAddress

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
