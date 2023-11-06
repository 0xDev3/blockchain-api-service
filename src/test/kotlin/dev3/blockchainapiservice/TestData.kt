package dev3.blockchainapiservice

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.JsonNodeConverter
import dev3.blockchainapiservice.util.UtcDateTime
import org.jooq.JSON
import java.time.OffsetDateTime

object TestData {
    val CHAIN_ID = ChainId(31337L)
    val EMPTY_JSON_OBJECT: JsonNode = JsonNodeConverter().from(JSON.valueOf("{}"))!!
    val EMPTY_JSON_ARRAY: ArrayNode = JsonNodeConverter().from(JSON.valueOf("[]"))!! as ArrayNode
    val TIMESTAMP: UtcDateTime = UtcDateTime(OffsetDateTime.parse("2022-02-02T00:00:00Z"))
}
