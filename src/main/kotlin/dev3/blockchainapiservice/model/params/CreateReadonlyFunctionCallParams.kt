package dev3.blockchainapiservice.model.params

import dev3.blockchainapiservice.exception.InvalidRequestBodyException
import dev3.blockchainapiservice.model.request.ReadonlyFunctionCallRequest
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.WalletAddress

data class CreateReadonlyFunctionCallParams(
    val identifier: DeployedContractIdentifier,
    val blockNumber: BlockNumber?,
    val functionCallInfo: ReadonlyFunctionCallInfo,
    val outputParams: List<OutputParameter>,
    val callerAddress: WalletAddress
) {
    companion object {
        private fun createFunctionCallInfo(requestBody: ReadonlyFunctionCallRequest): ReadonlyFunctionCallInfo {
            return if (
                requestBody.functionCallData != null &&
                (requestBody.functionName != null || requestBody.functionParams != null)
            ) {
                throw InvalidRequestBodyException(
                    "Cannot specify [function_call_data] when [function_name] or [function_params] are defined"
                )
            } else if (requestBody.functionCallData != null) {
                FunctionCallData(FunctionData(requestBody.functionCallData))
            } else if (requestBody.functionName != null && requestBody.functionParams != null) {
                FunctionNameAndParams(
                    functionName = requestBody.functionName,
                    functionParams = requestBody.functionParams
                )
            } else {
                throw InvalidRequestBodyException(
                    "Missing info to call readonly function: [function_name, function_params] or [function_call_data]"
                )
            }
        }
    }

    constructor(requestBody: ReadonlyFunctionCallRequest) : this(
        identifier = DeployedContractIdentifier(requestBody),
        blockNumber = requestBody.blockNumber?.let { BlockNumber(it) },
        functionCallInfo = createFunctionCallInfo(requestBody),
        outputParams = requestBody.outputParams,
        callerAddress = WalletAddress(requestBody.callerAddress)
    )
}

sealed interface ReadonlyFunctionCallInfo

data class FunctionNameAndParams(
    val functionName: String,
    val functionParams: List<FunctionArgument>,
) : ReadonlyFunctionCallInfo

data class FunctionCallData(
    val callData: FunctionData
) : ReadonlyFunctionCallInfo
