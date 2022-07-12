package com.ampnet.blockchainapiservice.util

private typealias Abi<T> = AbiType<T>

@JvmInline
value class AbiList<I, T : EthereumValue<I>>(val list: List<T>) : EthereumValue<List<I>> {
    override val rawValue: List<I>
        get() = list.map { it.rawValue }

    constructor(vararg elems: T) : this(elems.toList())
}

// based on org.web3j.abi.datatypes.AbiTypes
sealed class AbiType<T : EthereumValue<*>>(val name: String) {
    companion object AbiType {
        object Address : Abi<EthereumAddress>("address")
        object Uint256 : Abi<EthereumUint>("uint256")
        object Bool : Abi<EthereumValue<Boolean>>("bool")
        object Utf8String : Abi<EthereumString>("string")
        data class DynamicArray<I, T : EthereumValue<I>>(val arrayType: Abi<T>) :
            Abi<AbiList<I, T>>("${arrayType.name}[]")
    }
}
