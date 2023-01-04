package dev3.blockchainapiservice.features.contract.readcall.model.result

import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.UtcDateTime

data class ReadonlyFunctionCallResult(
    val blockNumber: BlockNumber,
    val timestamp: UtcDateTime,
    val rawReturnValue: String,
    val returnValues: List<Any>
)
