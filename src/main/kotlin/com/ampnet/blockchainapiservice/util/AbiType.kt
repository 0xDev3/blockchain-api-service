package com.ampnet.blockchainapiservice.util

import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Bool

interface AbiType {
    val typeReference: TypeReference<*>
}

enum class PrimitiveAbiType(override val typeReference: TypeReference<*>) : AbiType {
    BOOL(object : TypeReference<Bool>() {})
}
