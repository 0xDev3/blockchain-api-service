package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.AbiType
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder

@Service
class EthereumFunctionEncoderService : FunctionEncoderService {
    override fun encode(
        functionName: String,
        arguments: List<FunctionArgument<*, *>>,
        abiOutputTypes: List<AbiType<*>>
    ): FunctionData {
        val function = FunctionEncoder.makeFunction(
            functionName,
            arguments.map { it.abiType.name },
            arguments.map { it.value.rawValue },
            abiOutputTypes.map { it.name }
        )
        val data = FunctionEncoder.encode(function)
        return FunctionData(data)
    }

    override fun encodeConstructor(arguments: List<FunctionArgument<*, *>>): FunctionData {
        val inputParameters = FunctionEncoder.makeFunction(
            "constructor",
            arguments.map { it.abiType.name },
            arguments.map { it.value.rawValue },
            emptyList()
        ).inputParameters
        val data = FunctionEncoder.encodeConstructor(inputParameters)
        return FunctionData(data)
    }
}
