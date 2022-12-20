package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.features.promocodes.model.response.PromoCodeResponse
import dev3.blockchainapiservice.features.promocodes.model.response.PromoCodesResponse
import dev3.blockchainapiservice.features.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.promocodes.repository.PromoCodeRepository
import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.model.result.RequestUsage
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.security.WithMockUser
import dev3.blockchainapiservice.testcontainers.HardhatTestContainer
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

class PromoCodeControllerApiTest : ControllerTestBase() {

    companion object {
        private val USER_ID = UUID.randomUUID()
        private val MIN_TIME = UtcDateTime(OffsetDateTime.parse("1970-01-01T00:00:00Z"))
        private val MAX_TIME = UtcDateTime(OffsetDateTime.parse("9999-12-31T23:59:59Z"))
    }

    @Autowired
    private lateinit var userIdentifierRepository: UserIdentifierRepository

    @Autowired
    private lateinit var apiRateLimitRepository: ApiRateLimitRepository

    @Autowired
    private lateinit var promoCodeRepository: PromoCodeRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        userIdentifierRepository.store(
            UserWalletAddressIdentifier(
                id = USER_ID,
                stripeClientId = null,
                walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
            )
        )
    }

    @Test
    @WithMockUser
    fun mustCorrectlyGetPromoCodesForAllowedUser() {
        suppose("some promo codes exist in the repository") {
            promoCodeRepository.storeCode(
                code = "code-1",
                writeRequests = 1L,
                readRequests = 2L,
                validUntil = TestData.TIMESTAMP
            )
            promoCodeRepository.storeCode(
                code = "code-2",
                writeRequests = 10L,
                readRequests = 20L,
                validUntil = TestData.TIMESTAMP + 10.days
            )
        }

        val response = suppose("request to fetch promo codes is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/promo-codes")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, PromoCodesResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    PromoCodesResponse(
                        listOf(
                            PromoCodeResponse(
                                code = "code-2",
                                writeRequests = BigInteger("10"),
                                readRequests = BigInteger("20"),
                                numOfUsages = BigInteger.ZERO,
                                validUntil = TestData.TIMESTAMP.value + 10.days.toJavaDuration()
                            ),
                            PromoCodeResponse(
                                code = "code-1",
                                writeRequests = BigInteger.ONE,
                                readRequests = BigInteger.TWO,
                                numOfUsages = BigInteger.ZERO,
                                validUntil = TestData.TIMESTAMP.value
                            )
                        )
                    )
                )
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_2)
    fun mustCorrectlyGetPromoCodesForAllowedUserWithSomePeriodFilter() {
        suppose("some promo codes exist in the repository") {
            promoCodeRepository.storeCode(
                code = "code-1",
                writeRequests = 1L,
                readRequests = 2L,
                validUntil = TestData.TIMESTAMP
            )
            promoCodeRepository.storeCode(
                code = "code-2",
                writeRequests = 10L,
                readRequests = 20L,
                validUntil = TestData.TIMESTAMP + 10.days
            )
        }

        val from = TestData.TIMESTAMP - 1.days
        val to = TestData.TIMESTAMP + 1.days

        val response = suppose("request to fetch promo codes is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/promo-codes?from=${from.iso}&to=${to.iso}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, PromoCodesResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    PromoCodesResponse(
                        listOf(
                            PromoCodeResponse(
                                code = "code-1",
                                writeRequests = BigInteger.ONE,
                                readRequests = BigInteger.TWO,
                                numOfUsages = BigInteger.ZERO,
                                validUntil = TestData.TIMESTAMP.value
                            )
                        )
                    )
                )
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_3)
    fun mustNotGetPromoCodesForDisallowedUser() {
        verify("403 is returned for disallowed user") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/promo-codes")
            )
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.ACCESS_FORBIDDEN)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyGeneratePromoCodeForAllowedUser() {
        val response = suppose("request to generate promo code is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/promo-codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {}
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, PromoCodeResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    PromoCodeResponse(
                        code = response.code,
                        writeRequests = BigInteger.valueOf(5_000L),
                        readRequests = BigInteger.valueOf(1_000_000L),
                        numOfUsages = BigInteger.ZERO,
                        validUntil = response.validUntil
                    )
                )

            assertThat(response.code).withMessage()
                .startsWith("DEV3-")
            assertThat(response.validUntil - 30.days.toJavaDuration()).withMessage()
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("promo code was created") {
            val result = promoCodeRepository.getCodes(
                validFrom = MIN_TIME,
                validUntil = MAX_TIME
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        PromoCode(
                            code = response.code,
                            writeRequests = response.writeRequests.longValueExact(),
                            readRequests = response.readRequests.longValueExact(),
                            numOfUsages = 0L,
                            validUntil = result[0].validUntil
                        )
                    )
                )
            assertThat(result[0].validUntil.value).withMessage()
                .isCloseTo(response.validUntil, WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_2)
    fun mustCorrectlyGeneratePromoCodeForAllowedUserWithSomeParameters() {
        val response = suppose("request to generate promo code is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/promo-codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "prefix": "TEST-",
                                "write_requests": "1",
                                "read_requests": "2",
                                "validity_in_days": "3"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, PromoCodeResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    PromoCodeResponse(
                        code = response.code,
                        writeRequests = BigInteger.ONE,
                        readRequests = BigInteger.TWO,
                        numOfUsages = BigInteger.ZERO,
                        validUntil = response.validUntil
                    )
                )

            assertThat(response.code).withMessage()
                .startsWith("TEST-")
            assertThat(response.validUntil - 3.days.toJavaDuration()).withMessage()
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("promo code was created") {
            val result = promoCodeRepository.getCodes(
                validFrom = MIN_TIME,
                validUntil = MAX_TIME
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        PromoCode(
                            code = response.code,
                            writeRequests = response.writeRequests.longValueExact(),
                            readRequests = response.readRequests.longValueExact(),
                            numOfUsages = 0L,
                            validUntil = result[0].validUntil
                        )
                    )
                )
            assertThat(result[0].validUntil.value).withMessage()
                .isCloseTo(response.validUntil, WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_3)
    fun mustNotGeneratePromoCodeForDisallowedUser() {
        verify("403 is returned for disallowed user") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/promo-codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {}
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.ACCESS_FORBIDDEN)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyAllowUserToUsePromoCode() {
        suppose("some non-expired promo code exist in the repository") {
            promoCodeRepository.storeCode(
                code = "DEV3-TESTCD",
                writeRequests = 1L,
                readRequests = 2L,
                validUntil = UtcDateTime(OffsetDateTime.now()) + 30.days
            )
        }

        suppose("request to use promo code is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/use-promo-code/DEV3-TESTCD")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }

        verify("number of promo code usages was incremented to 1") {
            assertThat(
                promoCodeRepository.getCodes(
                    validFrom = MIN_TIME,
                    validUntil = MAX_TIME
                )[0].numOfUsages
            )
                .isOne()
        }

        verify("promo code is correctly used") {
            val currentPeriod = apiRateLimitRepository.getCurrentApiUsagePeriod(
                userId = USER_ID,
                currentTime = UtcDateTime(OffsetDateTime.now())
            )

            assertThat(currentPeriod).withMessage()
                .isEqualTo(
                    ApiUsagePeriod(
                        userId = USER_ID,
                        writeRequestUsage = RequestUsage(used = 0L, remaining = 1L),
                        readRequestUsage = RequestUsage(used = 0L, remaining = 2L),
                        startDate = currentPeriod.startDate,
                        endDate = currentPeriod.endDate
                    )
                )
            assertThat(currentPeriod.startDate.value).withMessage()
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(currentPeriod.endDate.value - 30.days.toJavaDuration()).withMessage()
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NoFoundForNonExistentPromoCode() {
        verify("404 is returned for non-existent promo code") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/use-promo-code/DEV3-TESTCD")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn400NoFoundForExpiredPromoCode() {
        suppose("some expired promo code exist in the repository") {
            promoCodeRepository.storeCode(
                code = "DEV3-TESTCD",
                writeRequests = 1L,
                readRequests = 2L,
                validUntil = UtcDateTime(OffsetDateTime.now()) - 30.days
            )
        }

        verify("400 is returned for expired promo code") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/use-promo-code/DEV3-TESTCD")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.PROMO_CODE_EXPIRED)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn400BadRequestForAlreadyUsedPromoCode() {
        suppose("some non-expired promo code exist in the repository") {
            promoCodeRepository.storeCode(
                code = "DEV3-TESTCD",
                writeRequests = 1L,
                readRequests = 2L,
                validUntil = UtcDateTime(OffsetDateTime.now()) + 30.days
            )
        }

        suppose("request to use promo code is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/use-promo-code/DEV3-TESTCD")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }

        verify("400 is returned for re-used promo code") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/use-promo-code/DEV3-TESTCD")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.PROMO_CODE_ALREADY_USED)
        }
    }
}
