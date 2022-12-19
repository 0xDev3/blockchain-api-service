package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.config.ApiRateProperties
import dev3.blockchainapiservice.generated.jooq.tables.ApiUsagePeriodTable
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiReadCallRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiUsagePeriodRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiWriteCallRecord
import dev3.blockchainapiservice.model.result.ApiUsageLimit
import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.model.result.RequestUsage
import dev3.blockchainapiservice.util.UtcDateTime
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.RequestMethod
import java.util.UUID
import kotlin.math.max
import dev3.blockchainapiservice.generated.jooq.enums.RequestMethod as DbRequestMethod

@Repository
@Suppress("TooManyFunctions")
class JooqApiRateLimitRepository(
    private val dslContext: DSLContext,
    private val apiRateProperties: ApiRateProperties
) : ApiRateLimitRepository {

    companion object : KLogging()

    override fun createNewFutureUsageLimits(userId: UUID, currentTime: UtcDateTime, limits: List<ApiUsageLimit>) {
        logger.info { "Create future API usage limits, userId: $userId, currentTime: $currentTime, limits: $limits" }

        // delete all future usage limits
        dslContext.deleteFrom(ApiUsagePeriodTable)
            .where(
                DSL.and(
                    ApiUsagePeriodTable.USER_ID.eq(userId),
                    ApiUsagePeriodTable.START_DATE.ge(currentTime)
                )
            )
            .execute()

        // end current usage period
        dslContext.update(ApiUsagePeriodTable)
            .set(ApiUsagePeriodTable.END_DATE, currentTime)
            .where(
                DSL.and(
                    ApiUsagePeriodTable.USER_ID.eq(userId),
                    ApiUsagePeriodTable.START_DATE.le(currentTime),
                    ApiUsagePeriodTable.END_DATE.ge(currentTime)
                )
            )
            .execute()

        val records = limits.map {
            ApiUsagePeriodRecord(
                id = UUID.randomUUID(),
                userId = userId,
                allowedWriteRequests = it.allowedWriteRequests,
                allowedReadRequests = it.allowedReadRequests,
                usedWriteRequests = 0L,
                usedReadRequests = 0L,
                startDate = it.startDate,
                endDate = it.endDate
            )
        }

        // insert new usage periods
        dslContext.batchInsert(records).execute()
    }

    override fun getCurrentApiUsagePeriod(userId: UUID, currentTime: UtcDateTime): ApiUsagePeriod {
        logger.debug { "Get current API usage period, userId: $userId, currentTime: $currentTime" }

        val currentPeriod = getOrCreateApiUsagePeriod(userId, currentTime)

        return ApiUsagePeriod(
            userId = userId,
            writeRequestUsage = calculateUsage(currentPeriod.usedWriteRequests, currentPeriod.allowedWriteRequests),
            readRequestUsage = calculateUsage(currentPeriod.usedReadRequests, currentPeriod.allowedReadRequests),
            startDate = currentPeriod.startDate,
            endDate = currentPeriod.endDate
        )
    }

    override fun remainingWriteLimit(userId: UUID, currentTime: UtcDateTime): Long {
        val currentPeriod = getOrCreateApiUsagePeriod(userId, currentTime)
        return calculateUsage(currentPeriod.usedWriteRequests, currentPeriod.allowedWriteRequests).remaining
    }

    override fun remainingReadLimit(userId: UUID, currentTime: UtcDateTime): Long {
        val currentPeriod = getOrCreateApiUsagePeriod(userId, currentTime)
        return calculateUsage(currentPeriod.usedReadRequests, currentPeriod.allowedReadRequests).remaining
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

        getOrCreateApiUsagePeriod(userId, currentTime).incrementField(ApiUsagePeriodTable.USED_WRITE_REQUESTS)
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

        getOrCreateApiUsagePeriod(userId, currentTime).incrementField(ApiUsagePeriodTable.USED_READ_REQUESTS)
    }

    private fun getOrCreateApiUsagePeriod(userId: UUID, currentTime: UtcDateTime): ApiUsagePeriodRecord =
        dslContext.selectFrom(ApiUsagePeriodTable)
            .where(
                DSL.and(
                    ApiUsagePeriodTable.USER_ID.eq(userId),
                    ApiUsagePeriodTable.START_DATE.le(currentTime),
                    ApiUsagePeriodTable.END_DATE.ge(currentTime)
                )
            )
            .orderBy(ApiUsagePeriodTable.END_DATE.desc())
            .limit(1)
            .fetchOne() ?: insertNewApiUsagePeriodRecord(userId, currentTime)

    private fun insertNewApiUsagePeriodRecord(userId: UUID, startDate: UtcDateTime): ApiUsagePeriodRecord {
        val endDate = startDate + apiRateProperties.usagePeriodDuration

        logger.info {
            "Creating API usage period for userId: $userId, period: [${startDate.value}, ${endDate.value}]"
        }

        val record = ApiUsagePeriodRecord(
            id = UUID.randomUUID(),
            userId = userId,
            allowedWriteRequests = apiRateProperties.freeTierWriteRequests,
            allowedReadRequests = apiRateProperties.freeTierReadRequests,
            usedWriteRequests = 0L,
            usedReadRequests = 0L,
            startDate = startDate,
            endDate = endDate
        )

        dslContext.executeInsert(record)

        return record
    }

    private fun ApiUsagePeriodRecord.incrementField(field: TableField<ApiUsagePeriodRecord, Long>) =
        dslContext.update(ApiUsagePeriodTable)
            .set(field, field.plus(1))
            .where(ApiUsagePeriodTable.ID.eq(this.id))
            .execute()

    private fun calculateUsage(count: Long, total: Long): RequestUsage =
        RequestUsage(
            used = count,
            remaining = max(total - count, 0)
        )
}
