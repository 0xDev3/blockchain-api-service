package com.ampnet.blockchainapiservice.util

import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.StaticArray
import org.web3j.abi.datatypes.Type

interface AbiType {
    val typeReference: TypeReference<*>
}

enum class PrimitiveAbiType(override val typeReference: TypeReference<*>) : AbiType {
    BOOL(object : TypeReference<Bool>() {})
}

class SizedStaticArray<T : Type<*>>(type: Class<T>, values: List<T>) : StaticArray<T>(type, values.size, values)
