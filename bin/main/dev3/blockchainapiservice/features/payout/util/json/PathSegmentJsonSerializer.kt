package dev3.blockchainapiservice.features.payout.util.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import dev3.blockchainapiservice.features.payout.util.MerkleTree.Companion.PathSegment

class PathSegmentJsonSerializer : JsonSerializer<PathSegment>() {

    override fun serialize(value: PathSegment, json: JsonGenerator, provider: SerializerProvider) {
        json.apply {
            writeStartObject()

            writeStringField("sibling_hash", value.siblingHash.value)
            writeBooleanField("is_left", value.isLeft)

            writeEndObject()
        }
    }
}
