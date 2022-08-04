package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData

interface FunctionEncoderService {
    fun encode(
        functionName: String,
        arguments: List<FunctionArgument>
    ): FunctionData

    fun encodeConstructor(arguments: List<FunctionArgument>): FunctionData
}
