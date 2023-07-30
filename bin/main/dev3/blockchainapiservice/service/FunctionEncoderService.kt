package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData

interface FunctionEncoderService {
    fun encode(
        functionName: String,
        arguments: List<FunctionArgument>
    ): FunctionData

    fun encodeConstructor(arguments: List<FunctionArgument>): FunctionData
}
