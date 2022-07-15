package com.ampnet.blockchainapiservice.util

import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String

@Suppress("DataClassPrivateConstructor")
data class FunctionArgument private constructor(
    val type: String = "", // ignored for now, used only in JSON schema generation
    val value: Type<*>
) {
    constructor(value: Type<*>) : this("", value)
    constructor(address: EthereumAddress) : this("", address.value)
    constructor(uint: EthereumUint) : this("", uint.value)
    constructor(string: String) : this("", Utf8String(string))
}
