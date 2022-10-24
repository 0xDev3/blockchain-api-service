package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.ApiKey
import java.util.UUID

interface ApiKeyRepository {
    fun store(apiKey: ApiKey): ApiKey
    fun getById(id: UUID): ApiKey?
    fun getByValue(value: String): ApiKey?
    fun getAllByProjectId(projectId: UUID): List<ApiKey>
    fun exists(apiKey: String): Boolean
}
