package dev3.blockchainapiservice.model.result

import java.util.UUID

data class ApiUsage(
    val userId: UUID,
    val usedWriteRequests: Long,
    val usedReadRequests: Long
)
