package com.ampnet.blockchainapiservice.util

private typealias Abi<T> = AbiType<T>

// based on org.web3j.abi.datatypes.AbiTypes
sealed class AbiType<T : EthereumValue<*>>(val name: String) {
    companion object AbiType {
        object Address : Abi<EthereumAddress>("address")
        object Uint256 : Abi<EthereumUint>("uint256")
        object Bool : Abi<EthereumValue<Boolean>>("bool")
        object Utf8String : Abi<EthereumString>("string")
    }
}
