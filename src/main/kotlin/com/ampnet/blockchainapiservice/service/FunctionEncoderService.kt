package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.AbiType
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData

interface FunctionEncoderService {
    fun encode(
        functionName: String,
        arguments: List<FunctionArgument<*, *>>,
        abiOutputTypes: List<AbiType<*>>
    ): FunctionData
}
