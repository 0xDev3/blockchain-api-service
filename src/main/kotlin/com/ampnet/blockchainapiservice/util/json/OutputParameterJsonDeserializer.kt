package com.ampnet.blockchainapiservice.util.json

import com.ampnet.blockchainapiservice.model.params.OutputParameter
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode

class OutputParameterJsonDeserializer : JsonDeserializer<OutputParameter>() {

    // TODO use more advanced deserialization here after implementing custom type decoder
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OutputParameter {
        val jsonTree = p.readValueAsTree<JsonNode>()

        if (jsonTree !is TextNode) {
            throw JsonParseException(p, "string expected")
        }

        val outputType = jsonTree.asText()

        return try {
            OutputParameter(outputType)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(p, "unknown type: $outputType")
        }
    }
}
