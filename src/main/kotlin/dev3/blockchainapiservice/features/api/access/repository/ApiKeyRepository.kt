package dev3.blockchainapiservice.features.api.access.repository

import dev3.blockchainapiservice.features.api.access.model.result.ApiKey
import dev3.blockchainapiservice.generated.jooq.id.ApiKeyId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId

interface ApiKeyRepository {
    fun store(apiKey: ApiKey): ApiKey
    fun getById(id: ApiKeyId): ApiKey?
    fun getByValue(value: String): ApiKey?
    fun getAllByProjectId(projectId: ProjectId): List<ApiKey>
    fun exists(apiKey: String): Boolean
}
