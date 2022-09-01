package com.ampnet.blockchainapiservice.util.json

import com.ampnet.blockchainapiservice.model.params.OutputParameter
import com.ampnet.blockchainapiservice.util.AbiType
import com.ampnet.blockchainapiservice.util.DynamicArrayType
import com.ampnet.blockchainapiservice.util.StaticArrayType
import com.ampnet.blockchainapiservice.util.StructType
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

typealias TypeResolver = (String) -> AbiType

class OutputParameterJsonDeserializer : JsonDeserializer<OutputParameter>() {

    companion object {
        private val ARRAY_REGEX_WITH_SIZE = "^(.+?)\\[(\\d*)]$".toRegex()
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OutputParameter {
        val jsonTree = p.readValueAsTree<JsonNode>()
        return OutputParameter(deserializeNode(p, jsonTree), jsonTree)
    }

    private fun deserializeNode(p: JsonParser, jsonTree: JsonNode): AbiType =
        when (jsonTree) {
            is TextNode -> deserializeType(p, jsonTree.asText()) {
                Web3TypeMappings.getAbiType(it)
                    ?: throw JsonParseException(p, "unknown type: $it")
            }

            is ObjectNode -> deserializeStruct(p, jsonTree)
            else -> throw JsonParseException(p, "invalid value type; expected string or object")
        }

    private fun deserializeType(p: JsonParser, outputType: String, typeResolver: TypeResolver): AbiType {
        val arrayMatchingResult = ARRAY_REGEX_WITH_SIZE.find(outputType)

        return if (arrayMatchingResult != null) {
            val (_, arrayElementType, arraySize) = arrayMatchingResult.groupValues
            parseArray(p, arrayElementType, arraySize.toIntOrNull(), typeResolver)
        } else {
            typeResolver(outputType)
        }
    }

    @Suppress("ThrowsCount")
    private fun deserializeStruct(p: JsonParser, jsonTree: ObjectNode): AbiType {
        val structArrayType = jsonTree["type"]
            ?.takeIf { it.isTextual }
            ?.asText() ?: throw JsonParseException(p, "missing struct type")

        return deserializeType(p, structArrayType) { type ->
            if (type != "struct") throw JsonParseException(p, "invalid struct type")
            jsonTree["elems"]
                ?.takeIf { it.isArray }?.elements()?.asSequence()
                ?.map { deserializeNode(p, it) }
                ?.toList()?.let { StructType(it) }
                ?: throw JsonParseException(p, "invalid or missing struct elements")
        }
    }

    private fun parseArray(p: JsonParser, elementType: String, length: Int?, typeResolver: TypeResolver): AbiType =
        deserializeType(p, elementType, typeResolver).let {
            when (length) {
                null -> DynamicArrayType(it)
                else -> StaticArrayType(it, length)
            }
        }
}
