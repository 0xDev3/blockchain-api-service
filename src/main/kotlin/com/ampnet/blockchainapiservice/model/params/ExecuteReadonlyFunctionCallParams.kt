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
    // TODO write extensive tests for parsing this
    val outputParameters: List<OutputParameter>
)

class OutputParameter(
    val solidityType: String,
    val typeReference: TypeReference<out Type<*>>
) {
    override fun equals(other: Any?): Boolean =
        other != null && (other is OutputParameter) && other.solidityType == solidityType &&
            other.typeReference.type.typeName == typeReference.type.typeName

    override fun hashCode(): Int = solidityType.hashCode()
}
