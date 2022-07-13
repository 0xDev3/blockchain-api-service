package com.ampnet.blockchainapiservice.util

import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String

@JvmInline
value class FunctionArgument(val value: Type<*>) {
    constructor(address: EthereumAddress) : this(address.value)
    constructor(uint: EthereumUint) : this(uint.value)
    constructor(string: String) : this(Utf8String(string))
}
