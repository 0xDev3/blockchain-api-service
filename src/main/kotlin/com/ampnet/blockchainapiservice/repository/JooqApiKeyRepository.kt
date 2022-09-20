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

    companion object : KLogging() {
        private val TABLE = ApiKeyTable.API_KEY
    }

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
        logger.debug { "Get API key by id: $id" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByValue(value: String): ApiKey? {
        logger.debug { "Get API key by value: $value" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.API_KEY_.eq(value))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: UUID): List<ApiKey> {
        logger.debug { "Get API keys by projectId: $projectId" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.PROJECT_ID.eq(projectId))
            .orderBy(TABLE.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun exists(apiKey: String): Boolean {
        logger.debug { "Check if API key exists: $apiKey" }
        return dslContext.fetchExists(
            TABLE,
            TABLE.API_KEY_.eq(apiKey)
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
