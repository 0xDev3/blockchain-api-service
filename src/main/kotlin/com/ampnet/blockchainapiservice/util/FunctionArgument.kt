package com.ampnet.blockchainapiservice.util

data class FunctionArgument<T : AbiType<V>, V : EthereumValue<*>>(val abiType: T, val value: V)
