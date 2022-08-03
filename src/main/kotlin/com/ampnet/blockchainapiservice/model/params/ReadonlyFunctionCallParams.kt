package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type

data class ReadonlyFunctionCallParams(
    val contractAddress: ContractAddress,
    val callerAddress: WalletAddress,
    val functionName: String,
    val functionData: FunctionData,
    // TODO write extensive tests for parsing this
    val outputParameters: List<TypeReference<out Type<*>>>
)
