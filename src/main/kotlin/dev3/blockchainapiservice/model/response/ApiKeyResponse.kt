package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.generated.jooq.id.ApiKeyId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.ApiKey
import java.time.OffsetDateTime

data class ApiKeyResponse(
    val id: ApiKeyId,
    val projectId: ProjectId,
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
