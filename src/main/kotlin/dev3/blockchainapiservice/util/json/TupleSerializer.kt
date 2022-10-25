package dev3.blockchainapiservice.util.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import dev3.blockchainapiservice.util.Tuple

class TupleSerializer : JsonSerializer<Tuple>() {
    override fun serialize(value: Tuple, gen: JsonGenerator, serializers: SerializerProvider) {
        serializers.findValueSerializer(List::class.java).serialize(value.elems, gen, serializers)
    }
}
