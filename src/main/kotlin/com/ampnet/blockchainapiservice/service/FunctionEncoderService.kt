package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import org.web3j.abi.datatypes.Type

interface FunctionEncoderService {
    fun encode(
        functionName: String,
        arguments: List<FunctionArgument>,
        abiOutputTypes: List<String>,
        additionalData: List<Type<*>>
    ): FunctionData
}
