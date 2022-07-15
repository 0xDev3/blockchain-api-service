package com.ampnet.blockchainapiservice.util.json

import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.web3j.abi.datatypes.Type

class FunctionArgumentJsonDeserializer : JsonDeserializer<FunctionArgument>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FunctionArgument {
        val jsonTree = p.readValueAsTree<TreeNode>()

        if (jsonTree !is ObjectNode) {
            throw JsonParseException(p, "object expected")
        }

        val argumentType = jsonTree["type"]?.asText() ?: throw JsonParseException(p, "missing type")
        val argumentValue = jsonTree["value"] ?: throw JsonParseException(p, "missing value")

        return FunctionArgument(deserializeType(p, argumentType, argumentValue))
    }

    private fun deserializeType(p: JsonParser, argumentType: String, argumentValue: JsonNode): Type<*> {
        // TODO handle and arrays structs
        return Web3TypeMappings[argumentType]
            ?.let { it(argumentValue, p) }
            ?: throw JsonParseException(p, "unknown type: $argumentType")
    }
}
