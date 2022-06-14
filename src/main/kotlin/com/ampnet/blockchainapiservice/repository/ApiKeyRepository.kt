package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.ApiKey
import java.util.UUID

interface ApiKeyRepository {
    fun getById(id: UUID): ApiKey?
    fun getAllByProjectId(projectId: UUID): List<ApiKey>
    fun create(apiKey: ApiKey): ApiKey
    fun exists(apiKey: String): Boolean
}
