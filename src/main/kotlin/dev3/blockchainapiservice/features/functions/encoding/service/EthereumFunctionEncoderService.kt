package dev3.blockchainapiservice.features.functions.encoding.service

import dev3.blockchainapiservice.features.functions.encoding.model.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Function

@Service
class EthereumFunctionEncoderService : FunctionEncoderService {
    override fun encode(
        functionName: String,
        arguments: List<FunctionArgument>
    ): FunctionData {
        val function = Function(
            functionName,
            arguments.map { it.value },
            emptyList()
        )
        val data = FunctionEncoder.encode(function)
        return FunctionData(data)
    }

    override fun encodeConstructor(arguments: List<FunctionArgument>): FunctionData {
        val data = FunctionEncoder.encodeConstructor(arguments.map { it.value })
        return FunctionData(data)
    }
}
