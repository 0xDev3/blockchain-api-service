package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IApiKeyRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import com.ampnet.blockchainapiservice.model.result.ApiKey
import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqApiKeyRepository(private val dslContext: DSLContext) : ApiKeyRepository {

    companion object : KLogging()

    override fun store(apiKey: ApiKey): ApiKey {
        logger.info { "Store API key, apiKey: $apiKey" }
        val record = ApiKeyRecord(
            id = apiKey.id,
            projectId = apiKey.projectId,
            apiKey = apiKey.apiKey,
            createdAt = apiKey.createdAt
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): ApiKey? {
        logger.info { "Get API key by id: $id" }
        return dslContext.selectFrom(ApiKeyTable.API_KEY)
            .where(ApiKeyTable.API_KEY.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: UUID): List<ApiKey> {
        logger.info { "Get API keys by projectId: $projectId" }
        return dslContext.selectFrom(ApiKeyTable.API_KEY)
            .where(ApiKeyTable.API_KEY.PROJECT_ID.eq(projectId))
            .orderBy(ApiKeyTable.API_KEY.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun exists(apiKey: String): Boolean {
        logger.info { "Check if API key exists: $apiKey" }
        return dslContext.fetchExists(
            ApiKeyTable.API_KEY,
            ApiKeyTable.API_KEY.API_KEY_.eq(apiKey)
        )
    }

    private fun IApiKeyRecord.toModel(): ApiKey =
        ApiKey(
            id = id!!,
            projectId = projectId!!,
            apiKey = apiKey!!,
            createdAt = createdAt!!
        )
}
