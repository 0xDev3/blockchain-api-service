package com.ampnet.blockchainapiservice.util

import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants
import com.ampnet.blockchainapiservice.util.annotation.SchemaIgnore
import com.fasterxml.jackson.databind.JsonNode
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String

@Suppress("DataClassPrivateConstructor")
data class FunctionArgument private constructor(
    val type: String = "", // ignored for now, used only in JSON schema generation
    val value: Type<*>,
    @SchemaIgnore
    @field:MaxJsonNodeChars(ValidationConstants.FUNCTION_ARGUMENT_MAX_JSON_CHARS)
    val rawJson: JsonNode? = null // needed to hold pre-deserialization value
) {
    constructor(value: Type<*>, rawJson: JsonNode?) : this("", value, rawJson)
    constructor(value: Type<*>) : this("", value, null)
    constructor(address: EthereumAddress) : this("", address.value)
    constructor(uint: EthereumUint) : this("", uint.value)
    constructor(string: String) : this("", Utf8String(string))
}
