package dev3.blockchainapiservice.util

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.validation.MaxJsonNodeChars
import dev3.blockchainapiservice.config.validation.ValidationConstants
import dev3.blockchainapiservice.util.annotation.SchemaAnyOf
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256

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

        fun fromUint256s(elems: List<EthereumUint>) =
            FunctionArgument(DynamicArray(Uint256::class.java, elems.map { Uint256(it.rawValue) }))
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
