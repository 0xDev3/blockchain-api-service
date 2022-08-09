package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type

data class ExecuteReadonlyFunctionCallParams(
    val contractAddress: ContractAddress,
    val callerAddress: WalletAddress,
    val functionName: String,
    val functionData: FunctionData,
    val outputParams: List<OutputParameter>
)

data class OutputParameter(val solidityType: String) {
    val typeReference: TypeReference<out Type<*>> = TypeReference.makeTypeReference(solidityType)
}
