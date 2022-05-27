package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.AbiType
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Type

@Service
class EthereumFunctionEncoderService : FunctionEncoderService {
    override fun encode(
        functionName: String,
        arguments: List<FunctionArgument<*, *>>,
        abiOutputTypes: List<AbiType<*>>,
        additionalData: List<Type<*>>
    ): FunctionData {
        val function = FunctionEncoder.makeFunction(
            functionName,
            arguments.map { it.abiType.name },
            arguments.map { it.value.rawValue },
            abiOutputTypes.map { it.name }
        )
        val data = FunctionEncoder.encode(function) + additionalData.joinToString { TypeEncoder.encode(it) }
        return FunctionData(data)
    }
}
