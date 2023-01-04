package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeAlreadyUsed
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeDoesNotExist
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeExpired
import dev3.blockchainapiservice.features.api.promocodes.repository.JooqPromoCodeRepository
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.generated.jooq.tables.PromoCodeTable
import dev3.blockchainapiservice.generated.jooq.tables.PromoCodeUsageTable
import dev3.blockchainapiservice.generated.jooq.tables.records.PromoCodeRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.util.UUID
import kotlin.time.Duration.Companion.days

@JooqTest
@Import(JooqPromoCodeRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqPromoCodeRepositoryIntegTest : TestBase() {

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqPromoCodeRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyStorePromoCode() {
        val promoCode = PromoCode(
            code = "test-code",
            writeRequests = 1L,
            readRequests = 2L,
            numOfUsages = 0L,
            validUntil = TestData.TIMESTAMP
        )

        val storedCode = suppose("promo code is stored into the database") {
            repository.storeCode(
                code = promoCode.code,
                writeRequests = promoCode.writeRequests,
                readRequests = promoCode.readRequests,
                validUntil = promoCode.validUntil
            )
        }

        verify("stored promo code is correctly returned") {
            expectThat(storedCode)
                .isEqualTo(promoCode)
        }

        verify("promo code is correctly stored into the database") {
            val result = dslContext.selectFrom(PromoCodeTable)
                .where(PromoCodeTable.CODE.eq(promoCode.code))
                .fetchOne { it.toModel() }

            expectThat(result)
                .isEqualTo(promoCode)
        }
    }

    @Test
    fun mustCorrectlyGetPromoCodesForSomeTimeFrame() {
        val oldCode = PromoCode(
            code = "old-code",
            writeRequests = 1L,
            readRequests = 2L,
            numOfUsages = 0L,
            validUntil = TestData.TIMESTAMP - 30.days
        )
        val currentCode = PromoCode(
            code = "current-code",
            writeRequests = 10L,
            readRequests = 20L,
            numOfUsages = 0L,
            validUntil = TestData.TIMESTAMP
        )
        val newCode = PromoCode(
            code = "new-code",
            writeRequests = 100L,
            readRequests = 200L,
            numOfUsages = 0L,
            validUntil = TestData.TIMESTAMP + 30.days
        )

        suppose("some promo codes are stored into the database") {
            repository.storeCode(
                code = oldCode.code,
                writeRequests = oldCode.writeRequests,
                readRequests = oldCode.readRequests,
                validUntil = oldCode.validUntil
            )
            repository.storeCode(
                code = currentCode.code,
                writeRequests = currentCode.writeRequests,
                readRequests = currentCode.readRequests,
                validUntil = currentCode.validUntil
            )
            repository.storeCode(
                code = newCode.code,
                writeRequests = newCode.writeRequests,
                readRequests = newCode.readRequests,
                validUntil = newCode.validUntil
            )
        }

        verify("correct promo code is fetched for some time frame") {
            val result = repository.getCodes(
                validFrom = TestData.TIMESTAMP - 15.days,
                validUntil = TestData.TIMESTAMP + 15.days
            )

            expectThat(result)
                .isEqualTo(listOf(currentCode))
        }
    }

    @Test
    fun mustCorrectlyUseValidPromoCode() {
        val promoCode = PromoCode(
            code = "test-code",
            writeRequests = 1L,
            readRequests = 2L,
            numOfUsages = 0L,
            validUntil = TestData.TIMESTAMP
        )

        suppose("some promo code exists") {
            repository.storeCode(
                code = promoCode.code,
                writeRequests = promoCode.writeRequests,
                readRequests = promoCode.readRequests,
                validUntil = promoCode.validUntil
            )
        }

        val userId = UserId(UUID.randomUUID())

        suppose("some user exists in the database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = userId,
                    userIdentifier = "user-identifier",
                    identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                    stripeClientId = null
                )
            )
        }

        verify("some valid promo code is used") {
            val result = repository.useCode(
                code = promoCode.code,
                userId = userId,
                currentTime = TestData.TIMESTAMP - 1.days
            )

            expectThat(result)
                .isEqualTo(promoCode.copy(numOfUsages = 1L))
        }

        verify("promo code usage record is inserted") {
            val result = dslContext.fetchExists(
                PromoCodeUsageTable,
                DSL.and(
                    PromoCodeUsageTable.USER_ID.eq(userId),
                    PromoCodeUsageTable.CODE.eq(promoCode.code)
                )
            )

            expectThat(result)
                .isTrue()
        }
    }

    @Test
    fun mustNotUsePromoCodeTwice() {
        val promoCode = PromoCode(
            code = "test-code",
            writeRequests = 1L,
            readRequests = 2L,
            numOfUsages = 0L,
            validUntil = TestData.TIMESTAMP
        )

        suppose("some promo code exists") {
            repository.storeCode(
                code = promoCode.code,
                writeRequests = promoCode.writeRequests,
                readRequests = promoCode.readRequests,
                validUntil = promoCode.validUntil
            )
        }

        val userId = UserId(UUID.randomUUID())

        suppose("some user exists in the database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = userId,
                    userIdentifier = "user-identifier",
                    identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                    stripeClientId = null
                )
            )
        }

        verify("some valid promo code is used") {
            val result = repository.useCode(
                code = promoCode.code,
                userId = userId,
                currentTime = TestData.TIMESTAMP - 1.days
            )

            expectThat(result)
                .isEqualTo(promoCode.copy(numOfUsages = 1L))
        }

        verify("already used promo code is not used again") {
            val result = repository.useCode(
                code = promoCode.code,
                userId = userId,
                currentTime = TestData.TIMESTAMP - 1.days
            )

            expectThat(result)
                .isEqualTo(PromoCodeAlreadyUsed)
        }

        verify("promo code usage record is inserted") {
            val result = dslContext.fetchExists(
                PromoCodeUsageTable,
                DSL.and(
                    PromoCodeUsageTable.USER_ID.eq(userId),
                    PromoCodeUsageTable.CODE.eq(promoCode.code)
                )
            )

            expectThat(result)
                .isTrue()
        }
    }

    @Test
    fun mustNotUseExpiredPromoCode() {
        val promoCode = PromoCode(
            code = "test-code",
            writeRequests = 1L,
            readRequests = 2L,
            numOfUsages = 0L,
            validUntil = TestData.TIMESTAMP
        )

        suppose("some promo code exists") {
            repository.storeCode(
                code = promoCode.code,
                writeRequests = promoCode.writeRequests,
                readRequests = promoCode.readRequests,
                validUntil = promoCode.validUntil
            )
        }

        val userId = UserId(UUID.randomUUID())

        suppose("some user exists in the database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = userId,
                    userIdentifier = "user-identifier",
                    identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                    stripeClientId = null
                )
            )
        }

        verify("some expired promo code is used") {
            val result = repository.useCode(
                code = promoCode.code,
                userId = userId,
                currentTime = TestData.TIMESTAMP + 1.days
            )

            expectThat(result)
                .isEqualTo(PromoCodeExpired)
        }

        verify("promo code usage record is not inserted") {
            val result = dslContext.fetchExists(
                PromoCodeUsageTable,
                DSL.and(
                    PromoCodeUsageTable.USER_ID.eq(userId),
                    PromoCodeUsageTable.CODE.eq(promoCode.code)
                )
            )

            expectThat(result)
                .isFalse()
        }
    }

    @Test
    fun mustNotUseNonExistentPromoCode() {
        val userId = UserId(UUID.randomUUID())

        suppose("some user exists in the database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = userId,
                    userIdentifier = "user-identifier",
                    identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                    stripeClientId = null
                )
            )
        }

        verify("some non-existent promo code is used") {
            val result = repository.useCode(
                code = "test",
                userId = userId,
                currentTime = TestData.TIMESTAMP + 1.days
            )

            expectThat(result)
                .isEqualTo(PromoCodeDoesNotExist)
        }

        verify("promo code usage record is not inserted") {
            val result = dslContext.fetchExists(
                PromoCodeUsageTable,
                DSL.and(
                    PromoCodeUsageTable.USER_ID.eq(userId),
                    PromoCodeUsageTable.CODE.eq("test")
                )
            )

            expectThat(result)
                .isFalse()
        }
    }

    private fun PromoCodeRecord.toModel() =
        PromoCode(
            code = code,
            writeRequests = writeRequests,
            readRequests = readRequests,
            numOfUsages = numOfUsages,
            validUntil = validUntil
        )
}
