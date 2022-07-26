package com.ampnet.blockchainapiservice.util.json

import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.SizedStaticArray
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.Type

class FunctionArgumentJsonDeserializer : JsonDeserializer<FunctionArgument>() {

    companion object {
        private const val ARRAY_VALUE_ERROR = "invalid value type; expected array"
        private val ARRAY_REGEX_WITH_SIZE = "^(.+?)\\[(\\d*)]$".toRegex()
        private val ARRAY_SUFFIX_REGEX = "^(.+?)(\\[\\d*])$".toRegex()
        private val OBJECT_MAPPER = ObjectMapper()
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FunctionArgument {
        val jsonTree = p.readValueAsTree<JsonNode>()

        if (jsonTree !is ObjectNode) {
            throw JsonParseException(p, "object expected")
        }

        val argumentType = jsonTree["type"]?.asText() ?: throw JsonParseException(p, "missing type")
        val argumentValue = jsonTree["value"] ?: throw JsonParseException(p, "missing value")

        return FunctionArgument(deserializeType(p, argumentType, argumentValue), jsonTree)
    }

    private fun deserializeType(p: JsonParser, argumentType: String, argumentValue: JsonNode): Type<*> {
        val arrayMatchingResult = ARRAY_REGEX_WITH_SIZE.find(argumentType)

        return if (arrayMatchingResult != null) {
            val (_, arrayElementType, arraySize) = arrayMatchingResult.groupValues
            argumentValue.parseArray(p, arrayElementType, arraySize.toIntOrNull())
        } else if (argumentType == "struct") {
            argumentValue.parseStruct(p)
        } else {
            Web3TypeMappings[argumentType]
                ?.let { it(argumentValue, p) }
                ?: throw JsonParseException(p, "unknown type: $argumentType")
        }
    }

    private fun JsonNode.parseArray(p: JsonParser, elementType: String, length: Int?): Type<*> =
        if (this.isArray) {
            this.elements().asSequence().map { Pair(it, deserializeType(p, elementType, it)) }.toList().takeIf {
                length == null || it.size == length
            }
                ?.checkStructCompatibility(p, elementType)
                ?.createArray(elementType.web3ElementType(p, this), length)
                ?: throw JsonParseException(p, "invalid array length")
        } else {
            throw JsonParseException(p, ARRAY_VALUE_ERROR)
        }

    @Suppress("UNCHECKED_CAST")
    private fun List<Type<*>>.createArray(web3ElementType: Class<out Type<*>>, length: Int?): Type<*> =
        if (length == null) {
            DynamicArray(web3ElementType as Class<Type<*>>, this)
        } else {
            SizedStaticArray(web3ElementType as Class<Type<*>>, this)
        }

    private fun List<Pair<JsonNode, Type<*>>>.checkStructCompatibility(
        p: JsonParser,
        elementType: String
    ): List<Type<*>> {
        if (elementType == "struct" && this.hasInvalidStructTypeHierarchy()) {
            throw JsonParseException(p, "mismatching struct elements in array")
        }

        return this.map { it.second }
    }

    private fun String.web3ElementType(p: JsonParser, node: JsonNode): Class<out Type<*>> =
        if (this.endsWith("[]")) {
            DynamicArray::class.java
        } else if (ARRAY_REGEX_WITH_SIZE.matches(this)) {
            SizedStaticArray::class.java
        } else if (this == "struct") {
            DynamicStruct::class.java
        } else {
            Web3TypeMappings.getWeb3Type(p, node, this)
                ?: throw JsonParseException(p, "unknown type: $this")
        }

    private fun JsonNode.parseStruct(p: JsonParser): DynamicStruct =
        if (this.isArray) {
            val structElements = this.elements().asSequence().map {
                val structArgumentType = it["type"]?.asText() ?: throw JsonParseException(p, "missing type")
                val structArgumentValue = it["value"] ?: throw JsonParseException(p, "missing value")
                deserializeType(p, structArgumentType, structArgumentValue)
            }.toList().takeIf { it.isNotEmpty() } ?: throw JsonParseException(p, "structs cannot be empty")

            DynamicStruct(structElements)
        } else {
            throw JsonParseException(p, ARRAY_VALUE_ERROR)
        }

    private fun List<Pair<JsonNode, Type<*>>>.hasInvalidStructTypeHierarchy() =
        this.map {
            getTypeHierarchy(
                OBJECT_MAPPER.createObjectNode().apply {
                    set<JsonNode>("value", it.first)
                    set<JsonNode>("type", textNode("struct"))
                }
            )
        }.toSet().size > 1

    internal fun getTypeHierarchy(node: JsonNode): String {
        val type = node["type"].asText()
        val arrayMatchingResult = ARRAY_SUFFIX_REGEX.find(type)

        return if (arrayMatchingResult != null) {
            val (_, arrayElementType, arraySuffix) = arrayMatchingResult.groupValues
            val arrayNode = OBJECT_MAPPER.createObjectNode().apply {
                set<JsonNode>(
                    "value",
                    node["value"].elements().asSequence().toList().getOrNull(0) ?: OBJECT_MAPPER.createArrayNode()
                )
                set<JsonNode>("type", textNode(arrayElementType))
            }

            getTypeHierarchy(arrayNode) + arraySuffix
        } else if (type == "struct") {
            node["value"].elements().asSequence().map { getTypeHierarchy(it) }
                .ifEmpty { sequenceOf("*") }
                .joinToString(prefix = "struct(", separator = ",", postfix = ")")
        } else {
            type
        }
    }
}
