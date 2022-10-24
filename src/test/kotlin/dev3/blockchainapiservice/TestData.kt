package com.ampnet.blockchainapiservice

import com.ampnet.blockchainapiservice.util.JsonNodeConverter
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.jooq.JSON
import java.time.OffsetDateTime

object TestData {
    val EMPTY_JSON_OBJECT: JsonNode = JsonNodeConverter().from(JSON.valueOf("{}"))!!
    val EMPTY_JSON_ARRAY: ArrayNode = JsonNodeConverter().from(JSON.valueOf("[]"))!! as ArrayNode
    val TIMESTAMP: UtcDateTime = UtcDateTime(OffsetDateTime.parse("2022-02-02T00:00:00Z"))
}
