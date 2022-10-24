package com.ampnet.blockchainapiservice.util.json

import com.ampnet.blockchainapiservice.util.Tuple
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class TupleSerializer : JsonSerializer<Tuple>() {
    override fun serialize(value: Tuple, gen: JsonGenerator, serializers: SerializerProvider) {
        serializers.findValueSerializer(List::class.java).serialize(value.elems, gen, serializers)
    }
}
