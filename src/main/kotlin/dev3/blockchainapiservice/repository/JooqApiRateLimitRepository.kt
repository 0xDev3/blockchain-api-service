package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.tables.ApiReadCallTable
import dev3.blockchainapiservice.generated.jooq.tables.ApiWriteCallTable
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiReadCallRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiWriteCallRecord
import dev3.blockchainapiservice.model.result.ApiUsage
import dev3.blockchainapiservice.util.UtcDateTime
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.RequestMethod
import java.util.UUID
import dev3.blockchainapiservice.generated.jooq.enums.RequestMethod as DbRequestMethod

@Repository
@Suppress("TooManyFunctions")
class JooqApiRateLimitRepository(private val dslContext: DSLContext) : ApiRateLimitRepository {

    companion object : KLogging()

    override fun getCurrentApiUsage(userId: UUID): ApiUsage {
        logger.debug { "Get current API usage period, userId: $userId" }

        return ApiUsage(
            userId = userId,
            usedWriteRequests = usedWriteRequests(userId),
            usedReadRequests = usedReadRequests(userId)
        )
    }

    override fun usedWriteRequests(userId: UUID): Long {
        val count = DSL.count().cast(Long::class.java)

        return dslContext.select(count)
            .from(ApiWriteCallTable)
            .where(ApiWriteCallTable.USER_ID.eq(userId))
            .fetchOne(count) ?: 0L
    }

    override fun usedReadRequests(userId: UUID): Long {
        val count = DSL.count().cast(Long::class.java)

        return dslContext.select(count)
            .from(ApiReadCallTable)
            .where(ApiReadCallTable.USER_ID.eq(userId))
            .fetchOne(count) ?: 0L
    }

    override fun addWriteCall(userId: UUID, currentTime: UtcDateTime, method: RequestMethod, endpoint: String) {
        logger.info {
            "Adding write call, userId: $userId, currentTime: $currentTime, method: $method, endpoint: $endpoint"
        }

        dslContext.executeInsert(
            ApiWriteCallRecord(
                userId = userId,
                requestMethod = DbRequestMethod.valueOf(method.name),
                requestPath = endpoint,
                createdAt = currentTime
            )
        )
    }

    override fun addReadCall(userId: UUID, currentTime: UtcDateTime, endpoint: String) {
        logger.info { "Adding read call, userId: $userId, currentTime: $currentTime, endpoint: $endpoint" }

        dslContext.executeInsert(
            ApiReadCallRecord(
                userId = userId,
                requestPath = endpoint,
                createdAt = currentTime
            )
        )
    }
}
