package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.util.UtcDateTime
import java.util.UUID

data class ApiKey(
    val id: UUID,
    val projectId: UUID,
    val apiKey: String,
    val createdAt: UtcDateTime
)
