package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.features.promo_codes.model.result.PromoCode
import dev3.blockchainapiservice.features.promo_codes.repository.JooqPromoCodeRepository
import dev3.blockchainapiservice.generated.jooq.tables.PromoCodeTable
import dev3.blockchainapiservice.generated.jooq.tables.records.PromoCodeRecord
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration

@JooqTest
@Import(JooqPromoCodeRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqPromoCodeIntegTest : TestBase() {

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
            assertThat(storedCode).withMessage()
                .isEqualTo(promoCode)
        }

        verify("promo code is correctly stored into the database") {
            val result = dslContext.selectFrom(PromoCodeTable)
                .where(PromoCodeTable.CODE.eq(promoCode.code))
                .fetchOne { it.toModel() }

            assertThat(result).withMessage()
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
            validUntil = TestData.TIMESTAMP - Duration.ofDays(30L)
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
            validUntil = TestData.TIMESTAMP + Duration.ofDays(30L)
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
                validFrom = TestData.TIMESTAMP - Duration.ofDays(15L),
                validUntil = TestData.TIMESTAMP + Duration.ofDays(15L)
            )

            assertThat(result).withMessage()
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

        verify("some valid promo code is used") {
            val result = repository.useCode(promoCode.code, TestData.TIMESTAMP - Duration.ofDays(1L))

            assertThat(result).withMessage()
                .isEqualTo(promoCode.copy(numOfUsages = 1L))
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

        verify("some expired promo code is used") {
            val result = repository.useCode(promoCode.code, TestData.TIMESTAMP + Duration.ofDays(1L))

            assertThat(result).withMessage()
                .isNull()
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
