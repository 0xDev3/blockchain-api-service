package dev3.blockchainapiservice.features.api.access.model.result

import dev3.blockchainapiservice.generated.jooq.id.ApiKeyId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.UtcDateTime

data class ApiKey(
    val id: ApiKeyId,
    val projectId: ProjectId,
    val apiKey: String,
    val createdAt: UtcDateTime
)
