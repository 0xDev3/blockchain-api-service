package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.id.ApiKeyId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.ApiKey

interface ApiKeyRepository {
    fun store(apiKey: ApiKey): ApiKey
    fun getById(id: ApiKeyId): ApiKey?
    fun getByValue(value: String): ApiKey?
    fun getAllByProjectId(projectId: ProjectId): List<ApiKey>
    fun exists(apiKey: String): Boolean
}
