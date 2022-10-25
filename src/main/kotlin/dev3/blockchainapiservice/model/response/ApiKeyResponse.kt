package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.model.result.ApiKey
import java.time.OffsetDateTime
import java.util.UUID

data class ApiKeyResponse(
    val id: UUID,
    val projectId: UUID,
    val apiKey: String,
    val createdAt: OffsetDateTime
) {
    constructor(apiKey: ApiKey) : this(
        id = apiKey.id,
        projectId = apiKey.projectId,
        apiKey = apiKey.apiKey,
        createdAt = apiKey.createdAt.value
    )
}
