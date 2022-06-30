package com.ampnet.blockchainapiservice.util

data class WithFunctionDataOrEthValue<T>(val value: T, val data: FunctionData?, val ethValue: Balance?)
