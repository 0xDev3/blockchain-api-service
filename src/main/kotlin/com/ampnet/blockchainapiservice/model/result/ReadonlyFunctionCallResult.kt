package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.UtcDateTime

data class ReadonlyFunctionCallResult(
    val blockNumber: BlockNumber,
    val timestamp: UtcDateTime,
    val returnValues: List<Any>
)
