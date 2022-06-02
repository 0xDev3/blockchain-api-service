package com.ampnet.blockchainapiservice

import com.ampnet.blockchainapiservice.util.JsonNodeConverter
import com.ampnet.blockchainapiservice.util.UtcDateTime
import org.jooq.JSON
import java.time.OffsetDateTime

object TestData {
    val EMPTY_JSON_OBJECT = JsonNodeConverter().from(JSON.valueOf("{}"))
    val TIMESTAMP = UtcDateTime(OffsetDateTime.parse("2022-02-02T00:00:00Z"))
}
