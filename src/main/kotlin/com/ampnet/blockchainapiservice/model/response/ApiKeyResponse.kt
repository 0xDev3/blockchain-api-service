package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.result.ApiKey
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
