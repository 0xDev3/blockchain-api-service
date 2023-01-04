package dev3.blockchainapiservice.features.functions.encoding.service

import dev3.blockchainapiservice.features.functions.encoding.model.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData

interface FunctionEncoderService {
    fun encode(
        functionName: String,
        arguments: List<FunctionArgument>
    ): FunctionData

    fun encodeConstructor(arguments: List<FunctionArgument>): FunctionData
}
