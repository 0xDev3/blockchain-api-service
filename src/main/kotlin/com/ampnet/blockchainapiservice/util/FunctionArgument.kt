package com.ampnet.blockchainapiservice.util

import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants
import com.ampnet.blockchainapiservice.util.annotation.SchemaAnyOf
import com.fasterxml.jackson.databind.JsonNode
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Uint
import org.web3j.abi.datatypes.Utf8String

@Suppress("DataClassPrivateConstructor")
data class FunctionArgument(
    val value: Type<*>,
    @field:MaxJsonNodeChars(ValidationConstants.FUNCTION_ARGUMENT_MAX_JSON_CHARS)
    val rawJson: JsonNode? = null // needed to hold pre-deserialization value
) {
    constructor(value: Type<*>) : this(value, null)
    constructor(address: EthereumAddress) : this(address.value)
    constructor(uint: EthereumUint) : this(uint.value)
    constructor(string: String) : this(Utf8String(string))

    companion object {
        fun fromAddresses(elems: List<EthereumAddress>) =
            FunctionArgument(DynamicArray(Address::class.java, elems.map { it.value }))

        fun fromUints(elems: List<EthereumUint>) =
            FunctionArgument(DynamicArray(Uint::class.java, elems.map { it.value }))
    }
}

data class FunctionArgumentSchema(
    val type: String,
    val value: FunctionArgumentTypes
)

data class FunctionArgumentTypes(
    val types: RecursiveFunctionArgumentTypes
)

@SchemaAnyOf
data class RecursiveFunctionArgumentTypes(
    val type1: String,
    val type2: Boolean,
    val type3: Number,
    val type4: List<StringOrNumber>,
    val type6: List<FunctionArgumentSchema>, // tuple type
    val type7: List<FunctionArgumentTypes> // nested lists
)

@SchemaAnyOf
data class StringOrNumber(
    val type1: String,
    val type2: Number
)
