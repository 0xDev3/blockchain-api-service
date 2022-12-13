package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.config.ApiRateProperties
import dev3.blockchainapiservice.generated.jooq.tables.ApiReadCallTable
import dev3.blockchainapiservice.generated.jooq.tables.ApiUsagePeriodTable
import dev3.blockchainapiservice.generated.jooq.tables.ApiWriteCallTable
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiReadCallRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiUsagePeriodRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiWriteCallRecord
import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.model.result.RequestUsage
import dev3.blockchainapiservice.util.UtcDateTime
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.RequestMethod
import java.util.UUID
import kotlin.math.max
import dev3.blockchainapiservice.generated.jooq.enums.RequestMethod as DbRequestMethod

@Repository
class JooqApiRateLimitRepository(
    private val dslContext: DSLContext,
    private val apiRateProperties: ApiRateProperties
) : ApiRateLimitRepository {

    companion object : KLogging()

    override fun getCurrentApiUsagePeriod(projectId: UUID, currentTime: UtcDateTime): ApiUsagePeriod {
        logger.debug { "Get current API usage period, projectId: $projectId, currentTime: $currentTime" }

        val currentPeriod = getOrCreateApiUsagePeriod(projectId, currentTime)

        val periodWriteCount = currentPeriod.countWriteCalls()
        val totalAllowedWrites = apiRateProperties.freeTierWriteRequests + currentPeriod.additionalWriteRequests

        val periodReadCount = currentPeriod.countReadCalls()
        val totalAllowedReads = apiRateProperties.freeTierReadRequests + currentPeriod.additionalReadRequests

        return ApiUsagePeriod(
            projectId = projectId,
            writeRequestUsage = calculateUsage(periodWriteCount, totalAllowedWrites),
            readRequestUsage = calculateUsage(periodReadCount, totalAllowedReads),
            startDate = currentPeriod.startDate,
            endDate = currentPeriod.endDate
        )
    }

    override fun remainingWriteLimit(projectId: UUID, currentTime: UtcDateTime): Int {
        val currentPeriod = getOrCreateApiUsagePeriod(projectId, currentTime)
        val periodWriteCount = currentPeriod.countWriteCalls()
        val totalAllowedWrites = apiRateProperties.freeTierWriteRequests + currentPeriod.additionalWriteRequests
        return calculateUsage(periodWriteCount, totalAllowedWrites).remaining
    }

    override fun remainingReadLimit(projectId: UUID, currentTime: UtcDateTime): Int {
        val currentPeriod = getOrCreateApiUsagePeriod(projectId, currentTime)
        val periodReadCount = currentPeriod.countReadCalls()
        val totalAllowedReads = apiRateProperties.freeTierReadRequests + currentPeriod.additionalReadRequests
        return calculateUsage(periodReadCount, totalAllowedReads).remaining
    }

    override fun addWriteCall(projectId: UUID, currentTime: UtcDateTime, method: RequestMethod, endpoint: String) {
        logger.info {
            "Adding write call, projectId: $projectId, currentTime: $currentTime, method: $method, endpoint: $endpoint"
        }

        dslContext.executeInsert(
            ApiWriteCallRecord(
                projectId = projectId,
                requestMethod = DbRequestMethod.valueOf(method.name),
                requestPath = endpoint,
                createdAt = currentTime
            )
        )
    }

    override fun addReadCall(projectId: UUID, currentTime: UtcDateTime, endpoint: String) {
        logger.info { "Adding read call, projectId: $projectId, currentTime: $currentTime, endpoint: $endpoint" }

        dslContext.executeInsert(
            ApiReadCallRecord(
                projectId = projectId,
                requestPath = endpoint,
                createdAt = currentTime
            )
        )
    }

    private fun getOrCreateApiUsagePeriod(projectId: UUID, currentTime: UtcDateTime): ApiUsagePeriodRecord =
        dslContext.selectFrom(ApiUsagePeriodTable)
            .where(
                DSL.and(
                    ApiUsagePeriodTable.PROJECT_ID.eq(projectId),
                    ApiUsagePeriodTable.END_DATE.ge(currentTime)
                )
            )
            .orderBy(ApiUsagePeriodTable.END_DATE.desc())
            .limit(1)
            .fetchOne() ?: insertNewApiUsagePeriodRecord(projectId, currentTime)

    private fun insertNewApiUsagePeriodRecord(projectId: UUID, startDate: UtcDateTime): ApiUsagePeriodRecord {
        val endDate = startDate + apiRateProperties.usagePeriodDuration

        logger.info {
            "Creating API usage period for projectId: $projectId, period: [${startDate.value}, ${endDate.value}]"
        }

        val record = ApiUsagePeriodRecord(
            projectId = projectId,
            additionalWriteRequests = 0,
            additionalReadRequests = 0,
            startDate = startDate,
            endDate = endDate
        )

        dslContext.executeInsert(record)

        return record
    }

    private fun ApiUsagePeriodRecord.countWriteCalls(): Int =
        dslContext.fetchCount(
            ApiWriteCallTable,
            DSL.and(
                ApiWriteCallTable.PROJECT_ID.eq(projectId),
                ApiWriteCallTable.CREATED_AT.between(startDate, endDate)
            )
        )

    private fun ApiUsagePeriodRecord.countReadCalls(): Int =
        dslContext.fetchCount(
            ApiReadCallTable,
            DSL.and(
                ApiReadCallTable.PROJECT_ID.eq(projectId),
                ApiReadCallTable.CREATED_AT.between(startDate, endDate)
            )
        )

    private fun calculateUsage(count: Int, total: Int): RequestUsage =
        RequestUsage(
            used = count,
            remaining = max(total - count, 0)
        )
}
