package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.ApiRateProperties
import dev3.blockchainapiservice.features.api.usage.model.result.ApiUsageLimit
import dev3.blockchainapiservice.features.api.usage.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.features.api.usage.model.result.RequestUsage
import dev3.blockchainapiservice.features.api.usage.repository.JooqApiRateLimitRepository
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.id.ApiUsagePeriodId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.generated.jooq.tables.ApiUsagePeriodTable
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiUsagePeriodRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.web.bind.annotation.RequestMethod
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.toKotlinDuration

@JooqTest
@Import(JooqApiRateLimitRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableConfigurationProperties(ApiRateProperties::class)
class JooqApiRateLimitRepositoryIntegTest : TestBase() {

    companion object {
        private val USER_ID = UserId(UUID.randomUUID())
        private val DEFAULT_PROPERTIES = ApiRateProperties()
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqApiRateLimitRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = USER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                stripeClientId = null
            )
        )
    }

    @Test
    fun mustCorrectlyCreateFutureUsageLimits() {
        suppose("some current and future usage periods exist for the user") {
            dslContext.executeInsert(
                ApiUsagePeriodRecord( // past, should be kept
                    id = ApiUsagePeriodId(UUID.randomUUID()),
                    userId = USER_ID,
                    allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                    allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                    usedWriteRequests = 0L,
                    usedReadRequests = 0L,
                    startDate = TestData.TIMESTAMP - DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration(),
                    endDate = TestData.TIMESTAMP
                )
            )
            dslContext.executeInsert(
                ApiUsagePeriodRecord( // current, should be ended
                    id = ApiUsagePeriodId(UUID.randomUUID()),
                    userId = USER_ID,
                    allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                    allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                    usedWriteRequests = 0L,
                    usedReadRequests = 0L,
                    startDate = TestData.TIMESTAMP,
                    endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                )
            )
            dslContext.executeInsert(
                ApiUsagePeriodRecord( // future, should be deleted
                    id = ApiUsagePeriodId(UUID.randomUUID()),
                    userId = USER_ID,
                    allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                    allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                    usedWriteRequests = 0L,
                    usedReadRequests = 0L,
                    startDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration(),
                    endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration() * 2
                )
            )
        }

        suppose("some future usage period is created") {
            repository.createNewFutureUsageLimits(
                userId = USER_ID,
                currentTime = TestData.TIMESTAMP + 1.days,
                limits = listOf(
                    ApiUsageLimit(
                        userId = USER_ID,
                        allowedWriteRequests = 1L,
                        allowedReadRequests = 2L,
                        startDate = TestData.TIMESTAMP + 1.days,
                        endDate = TestData.TIMESTAMP + 2.days
                    )
                )
            )
        }

        verify("future usage periods are deleted and current one is ended") {
            val result = dslContext.selectFrom(ApiUsagePeriodTable)
                .where(ApiUsagePeriodTable.USER_ID.eq(USER_ID))
                .orderBy(ApiUsagePeriodTable.END_DATE.desc())
                .fetch {
                    ApiUsageLimit(
                        userId = it.userId,
                        allowedWriteRequests = it.allowedWriteRequests,
                        allowedReadRequests = it.allowedReadRequests,
                        startDate = it.startDate,
                        endDate = it.endDate
                    )
                }

            expectThat(result)
                .isEqualTo(
                    listOf(
                        ApiUsageLimit( // new
                            userId = USER_ID,
                            allowedWriteRequests = 1L,
                            allowedReadRequests = 2L,
                            startDate = TestData.TIMESTAMP + 1.days,
                            endDate = TestData.TIMESTAMP + 2.days
                        ),
                        ApiUsageLimit( // ended current
                            userId = USER_ID,
                            allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                            allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                            startDate = TestData.TIMESTAMP,
                            endDate = TestData.TIMESTAMP + 1.days
                        ),
                        ApiUsageLimit( // past
                            userId = USER_ID,
                            allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                            allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                            startDate = TestData.TIMESTAMP - DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration(),
                            endDate = TestData.TIMESTAMP
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyGetCurrentUsagePeriodWhenThereAreNoPreviousPeriods() {
        val usagePeriod = suppose("current API usage period is requested") {
            repository.getCurrentApiUsagePeriod(USER_ID, TestData.TIMESTAMP)
        }

        verify("period has correct values") {
            expectThat(usagePeriod)
                .isEqualTo(
                    ApiUsagePeriod(
                        userId = USER_ID,
                        writeRequestUsage = RequestUsage(
                            used = 0,
                            remaining = DEFAULT_PROPERTIES.freeTierWriteRequests
                        ),
                        readRequestUsage = RequestUsage(
                            used = 0,
                            remaining = DEFAULT_PROPERTIES.freeTierReadRequests
                        ),
                        startDate = TestData.TIMESTAMP,
                        endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                    )
                )
        }

        verify("new usage period is created") {
            val record = dslContext.selectFrom(ApiUsagePeriodTable)
                .where(ApiUsagePeriodTable.USER_ID.eq(USER_ID))
                .fetchOne()

            expectThat(record)
                .isEqualTo(
                    ApiUsagePeriodRecord(
                        id = record!!.id,
                        userId = USER_ID,
                        allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                        allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                        usedWriteRequests = 0L,
                        usedReadRequests = 0L,
                        startDate = TestData.TIMESTAMP,
                        endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyGetCurrentUsagePeriodWhenThereIsSomeTooOldPreviousPeriod() {
        suppose("some too old usage period exists for the user") {
            dslContext.executeInsert(
                ApiUsagePeriodRecord(
                    id = ApiUsagePeriodId(UUID.randomUUID()),
                    userId = USER_ID,
                    allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                    allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                    usedWriteRequests = 0L,
                    usedReadRequests = 0L,
                    startDate = TestData.TIMESTAMP - DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration() * 3,
                    endDate = TestData.TIMESTAMP - DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration() * 2
                )
            )
        }

        val usagePeriod = suppose("current API usage period is requested") {
            repository.getCurrentApiUsagePeriod(USER_ID, TestData.TIMESTAMP)
        }

        verify("period has correct values") {
            expectThat(usagePeriod)
                .isEqualTo(
                    ApiUsagePeriod(
                        userId = USER_ID,
                        writeRequestUsage = RequestUsage(
                            used = 0,
                            remaining = DEFAULT_PROPERTIES.freeTierWriteRequests
                        ),
                        readRequestUsage = RequestUsage(
                            used = 0,
                            remaining = DEFAULT_PROPERTIES.freeTierReadRequests
                        ),
                        startDate = TestData.TIMESTAMP,
                        endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                    )
                )
        }

        verify("new usage period is created") {
            val record = dslContext.selectFrom(ApiUsagePeriodTable)
                .where(ApiUsagePeriodTable.USER_ID.eq(USER_ID))
                .orderBy(ApiUsagePeriodTable.END_DATE.desc())
                .limit(1)
                .fetchOne()

            expectThat(record)
                .isEqualTo(
                    ApiUsagePeriodRecord(
                        id = record!!.id,
                        userId = USER_ID,
                        allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                        allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                        usedWriteRequests = 0L,
                        usedReadRequests = 0L,
                        startDate = TestData.TIMESTAMP,
                        endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyGetCurrentUsagePeriodWhenThereIsSomeCurrentUsagePeriod() {
        suppose("some current usage period exists for the user") {
            dslContext.executeInsert(
                ApiUsagePeriodRecord(
                    id = ApiUsagePeriodId(UUID.randomUUID()),
                    userId = USER_ID,
                    allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                    allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                    usedWriteRequests = 0L,
                    usedReadRequests = 0L,
                    startDate = TestData.TIMESTAMP,
                    endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                )
            )
        }

        val timestamp = TestData.TIMESTAMP + 1.days

        suppose("some read and write requests exist inside current period") {
            repository.addReadCall(USER_ID, timestamp, "/test")
            repository.addWriteCall(USER_ID, timestamp, RequestMethod.POST, "/test")
            repository.addWriteCall(USER_ID, timestamp, RequestMethod.POST, "/test")
        }

        val usagePeriod = suppose("current API usage period is requested") {
            repository.getCurrentApiUsagePeriod(USER_ID, timestamp)
        }

        verify("period has correct values") {
            expectThat(usagePeriod)
                .isEqualTo(
                    ApiUsagePeriod(
                        userId = USER_ID,
                        writeRequestUsage = RequestUsage(
                            used = 2,
                            remaining = DEFAULT_PROPERTIES.freeTierWriteRequests - 2
                        ),
                        readRequestUsage = RequestUsage(
                            used = 1,
                            remaining = DEFAULT_PROPERTIES.freeTierReadRequests - 1
                        ),
                        startDate = TestData.TIMESTAMP,
                        endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                    )
                )
        }

        verify("no new usage period is created") {
            val count = dslContext.fetchCount(ApiUsagePeriodTable, ApiUsagePeriodTable.USER_ID.eq(USER_ID))

            expectThat(count)
                .isOne()
        }
    }

    @Test
    fun mustCorrectlyFetchRemainingWriteLimitWhenThereIsNoCurrentPeriod() {
        suppose("some write requests exist inside current period") {
            repository.addWriteCall(USER_ID, TestData.TIMESTAMP, RequestMethod.POST, "/test")
            repository.addWriteCall(USER_ID, TestData.TIMESTAMP, RequestMethod.POST, "/test")
        }

        verify("remaining rate limit is correctly fetched") {
            val remainingLimit = repository.remainingWriteLimit(USER_ID, TestData.TIMESTAMP)

            expectThat(remainingLimit)
                .isEqualTo(DEFAULT_PROPERTIES.freeTierWriteRequests - 2)
        }
    }

    @Test
    fun mustCorrectlyFetchRemainingWriteLimitForCurrentPeriod() {
        suppose("some current usage period exists for the user") {
            dslContext.executeInsert(
                ApiUsagePeriodRecord(
                    id = ApiUsagePeriodId(UUID.randomUUID()),
                    userId = USER_ID,
                    allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                    allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                    usedWriteRequests = 0L,
                    usedReadRequests = 0L,
                    startDate = TestData.TIMESTAMP - DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration(),
                    endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                )
            )
        }

        suppose("some write requests exist inside current period") {
            repository.addWriteCall(USER_ID, TestData.TIMESTAMP, RequestMethod.POST, "/test")
            repository.addWriteCall(USER_ID, TestData.TIMESTAMP, RequestMethod.POST, "/test")
        }

        verify("remaining rate limit is correctly fetched") {
            val remainingLimit = repository.remainingWriteLimit(USER_ID, TestData.TIMESTAMP - 1.days)

            expectThat(remainingLimit)
                .isEqualTo(DEFAULT_PROPERTIES.freeTierWriteRequests - 2)
        }
    }

    @Test
    fun mustCorrectlyFetchRemainingReadLimitWhenThereIsNoCurrentPeriod() {
        suppose("some read requests exist inside current period") {
            repository.addReadCall(USER_ID, TestData.TIMESTAMP, "/test")
            repository.addReadCall(USER_ID, TestData.TIMESTAMP, "/test")
        }

        verify("remaining rate limit is correctly fetched") {
            val remainingLimit = repository.remainingReadLimit(USER_ID, TestData.TIMESTAMP)

            expectThat(remainingLimit)
                .isEqualTo(DEFAULT_PROPERTIES.freeTierReadRequests - 2)
        }
    }

    @Test
    fun mustCorrectlyFetchRemainingReadLimitForCurrentPeriod() {
        suppose("some current usage period exists for the user") {
            dslContext.executeInsert(
                ApiUsagePeriodRecord(
                    id = ApiUsagePeriodId(UUID.randomUUID()),
                    userId = USER_ID,
                    allowedWriteRequests = DEFAULT_PROPERTIES.freeTierWriteRequests,
                    allowedReadRequests = DEFAULT_PROPERTIES.freeTierReadRequests,
                    usedWriteRequests = 0L,
                    usedReadRequests = 0L,
                    startDate = TestData.TIMESTAMP - DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration(),
                    endDate = TestData.TIMESTAMP + DEFAULT_PROPERTIES.usagePeriodDuration.toKotlinDuration()
                )
            )
        }

        suppose("some read requests exist inside current period") {
            repository.addReadCall(USER_ID, TestData.TIMESTAMP, "/test")
            repository.addReadCall(USER_ID, TestData.TIMESTAMP, "/test")
        }

        verify("remaining rate limit is correctly fetched") {
            val remainingLimit = repository.remainingReadLimit(USER_ID, TestData.TIMESTAMP - 1.days)

            expectThat(remainingLimit)
                .isEqualTo(DEFAULT_PROPERTIES.freeTierReadRequests - 2)
        }
    }
}
