package dev3.blockchainapiservice.features.payout.util.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import dev3.blockchainapiservice.features.payout.util.MerkleHash

class MerkleHashJsonSerializer : JsonSerializer<MerkleHash>() {

    override fun serialize(hash: MerkleHash, json: JsonGenerator, provider: SerializerProvider) =
        json.writeString(hash.value)
}
