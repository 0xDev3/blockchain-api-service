package com.ampnet.blockchainapiservice

import com.ampnet.blockchainapiservice.util.JsonNodeConverter
import org.jooq.JSON

object TestData {
    val EMPTY_JSON_OBJECT = JsonNodeConverter().from(JSON.valueOf("{}"))
}
