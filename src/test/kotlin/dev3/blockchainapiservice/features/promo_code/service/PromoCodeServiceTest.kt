package dev3.blockchainapiservice.features.promo_code.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.PromoCodeProperties
import dev3.blockchainapiservice.exception.AccessForbiddenException
import dev3.blockchainapiservice.exception.PromoCodeAlreadyUsedException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.exception.SubscriptionAlreadyActiveException
import dev3.blockchainapiservice.features.billing.service.StripeBillingService
import dev3.blockchainapiservice.features.promo_codes.model.result.PromoCode
import dev3.blockchainapiservice.features.promo_codes.model.result.PromoCodeAlreadyUsed
import dev3.blockchainapiservice.features.promo_codes.model.result.PromoCodeDoesNotExist
import dev3.blockchainapiservice.features.promo_codes.repository.PromoCodeRepository
import dev3.blockchainapiservice.features.promo_codes.service.PromoCodeServiceImpl
import dev3.blockchainapiservice.model.result.ApiUsageLimit
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.service.RandomProvider
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import java.time.Duration
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class PromoCodeServiceTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = "client-id",
            walletAddress = WalletAddress("abc")
        )
        private val PROMO_CODE = PromoCode(
            code = "DEV3-ABCDEF",
            writeRequests = 1L,
            readRequests = 2L,
            numOfUsages = 0L,
            validUntil = TestData.TIMESTAMP
        )
    }

    @Test
    fun mustCorrectlyGeneratePromoCodeWhenUserIsAllowedToGeneratePromoCode() {
        val promoCodeProperties = suppose("user is allowed to generate promo code") {
            PromoCodeProperties(allowedWallets = setOf(USER_IDENTIFIER.walletAddress))
        }

        val randomProvider = mock<RandomProvider>()

        suppose("some promo code will be generated") {
            given(randomProvider.getBytes(6))
                .willReturn(ByteArray(6) { it.toByte() })
        }

        val promoCodeRepository = mock<PromoCodeRepository>()


        suppose("promo code will be stored into the database") {
            given(
                promoCodeRepository.storeCode(
                    code = PROMO_CODE.code,
                    writeRequests = PROMO_CODE.writeRequests,
                    readRequests = PROMO_CODE.readRequests,
                    validUntil = PROMO_CODE.validUntil
                )
            )
                .willReturn(PROMO_CODE)
        }

        val service = PromoCodeServiceImpl(
            billingService = mock(),
            promoCodeRepository = promoCodeRepository,
            apiRateLimitRepository = mock(),
            randomProvider = randomProvider,
            utcDateTimeProvider = mock(),
            promoCodeProperties = promoCodeProperties
        )

        verify("promo code is correctly generated and stored") {
            val result = service.generatePromoCode(
                userIdentifier = USER_IDENTIFIER,
                prefix = "DEV3-",
                writeRequests = PROMO_CODE.writeRequests,
                readRequests = PROMO_CODE.readRequests,
                validUntil = TestData.TIMESTAMP
            )

            assertThat(result).withMessage()
                .isEqualTo(PROMO_CODE)
        }
    }

    @Test
    fun mustThrowAccessForbiddenExceptionWhenUserIsNotAllowedToGeneratePromoCode() {
        val promoCodeProperties = suppose("user is not allowed to generate promo code") {
            PromoCodeProperties()
        }

        val service = PromoCodeServiceImpl(
            billingService = mock(),
            promoCodeRepository = mock(),
            apiRateLimitRepository = mock(),
            randomProvider = mock(),
            utcDateTimeProvider = mock(),
            promoCodeProperties = promoCodeProperties
        )

        verify("AccessForbiddenException is thrown") {
            assertThrows<AccessForbiddenException>(message) {
                service.generatePromoCode(
                    userIdentifier = USER_IDENTIFIER,
                    prefix = "DEV3-",
                    writeRequests = 1L,
                    readRequests = 2L,
                    validUntil = TestData.TIMESTAMP
                )
            }
        }
    }

    @Test
    fun mustCorrectlyFetchPromoCodesWhenUserIsAllowedToFetchPromoCodes() {
        val promoCodeProperties = suppose("user is allowed to generate promo code") {
            PromoCodeProperties(allowedWallets = setOf(USER_IDENTIFIER.walletAddress))
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo codes will be returned") {
            given(promoCodeRepository.getCodes(TestData.TIMESTAMP, TestData.TIMESTAMP + Duration.ofDays(1L)))
                .willReturn(listOf(PROMO_CODE))
        }

        val service = PromoCodeServiceImpl(
            billingService = mock(),
            promoCodeRepository = promoCodeRepository,
            apiRateLimitRepository = mock(),
            randomProvider = mock(),
            utcDateTimeProvider = mock(),
            promoCodeProperties = promoCodeProperties
        )

        verify("promo codes are correctly fetched") {
            val result = service.getPromoCodes(
                userIdentifier = USER_IDENTIFIER,
                validFrom = TestData.TIMESTAMP,
                validUntil = TestData.TIMESTAMP + Duration.ofDays(1L)
            )

            assertThat(result).withMessage()
                .isEqualTo(listOf(PROMO_CODE))
        }
    }

    @Test
    fun mustThrowAccessForbiddenExceptionWhenUserIsNotAllowedToFetchPromoCodes() {
        val promoCodeProperties = suppose("user is not allowed to generate promo code") {
            PromoCodeProperties()
        }

        val service = PromoCodeServiceImpl(
            billingService = mock(),
            promoCodeRepository = mock(),
            apiRateLimitRepository = mock(),
            randomProvider = mock(),
            utcDateTimeProvider = mock(),
            promoCodeProperties = promoCodeProperties
        )

        verify("AccessForbiddenException is thrown") {
            assertThrows<AccessForbiddenException>(message) {
                service.getPromoCodes(
                    userIdentifier = USER_IDENTIFIER,
                    validFrom = TestData.TIMESTAMP,
                    validUntil = TestData.TIMESTAMP + Duration.ofDays(1L)
                )
            }
        }
    }

    @Test
    fun mustAllowUserToUsePromoCode() {
        val billingService = mock<StripeBillingService>()

        suppose("user does not have an active subscription") {
            given(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(false)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo code is used") {
            given(
                promoCodeRepository.useCode(
                    code = PROMO_CODE.code,
                    userId = USER_IDENTIFIER.id,
                    currentTime = TestData.TIMESTAMP
                )
            )
                .willReturn(PROMO_CODE)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val service = PromoCodeServiceImpl(
            billingService = billingService,
            promoCodeRepository = promoCodeRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            randomProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider,
            promoCodeProperties = PromoCodeProperties()
        )

        verify("promo code is used and api rate limit is added to the user") {
            service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)

            verifyMock(apiRateLimitRepository)
                .createNewFutureUsageLimits(
                    userId = USER_IDENTIFIER.id,
                    currentTime = TestData.TIMESTAMP,
                    limits = listOf(
                        ApiUsageLimit(
                            userId = USER_IDENTIFIER.id,
                            allowedWriteRequests = PROMO_CODE.writeRequests,
                            allowedReadRequests = PROMO_CODE.readRequests,
                            startDate = TestData.TIMESTAMP,
                            endDate = TestData.TIMESTAMP + Duration.ofDays(30L)
                        )
                    )
                )
            verifyNoMoreInteractions(apiRateLimitRepository)
        }
    }

    @Test
    fun mustThrowSubscriptionAlreadyActiveExceptionWhenUserIsTryingToClaimPromoCodeAndHasActiveSubscription() {
        val billingService = mock<StripeBillingService>()

        suppose("user has an active subscription") {
            given(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(true)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val service = PromoCodeServiceImpl(
            billingService = billingService,
            promoCodeRepository = mock(),
            apiRateLimitRepository = apiRateLimitRepository,
            randomProvider = mock(),
            utcDateTimeProvider = mock(),
            promoCodeProperties = PromoCodeProperties()
        )

        verify("SubscriptionAlreadyActiveException is thrown") {
            assertThrows<SubscriptionAlreadyActiveException>(message) {
                service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)
            }

            verifyNoInteractions(apiRateLimitRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenPromoCodeDoesNotExist() {
        val billingService = mock<StripeBillingService>()

        suppose("user does not have an active subscription") {
            given(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(false)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo code does not exist") {
            given(
                promoCodeRepository.useCode(
                    code = PROMO_CODE.code,
                    userId = USER_IDENTIFIER.id,
                    currentTime = TestData.TIMESTAMP
                )
            )
                .willReturn(PromoCodeDoesNotExist)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val service = PromoCodeServiceImpl(
            billingService = billingService,
            promoCodeRepository = promoCodeRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            randomProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider,
            promoCodeProperties = PromoCodeProperties()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)
            }

            verifyNoInteractions(apiRateLimitRepository)
        }
    }

    @Test
    fun mustThrowPromoCodeAlreadyUsedExceptionWhenUserHasAlreadyUsedSomePromoCode() {
        val billingService = mock<StripeBillingService>()

        suppose("user does not have an active subscription") {
            given(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(false)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo code was already used") {
            given(
                promoCodeRepository.useCode(
                    code = PROMO_CODE.code,
                    userId = USER_IDENTIFIER.id,
                    currentTime = TestData.TIMESTAMP
                )
            )
                .willReturn(PromoCodeAlreadyUsed)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val service = PromoCodeServiceImpl(
            billingService = billingService,
            promoCodeRepository = promoCodeRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            randomProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider,
            promoCodeProperties = PromoCodeProperties()
        )

        verify("PromoCodeAlreadyUsedException is thrown") {
            assertThrows<PromoCodeAlreadyUsedException>(message) {
                service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)
            }

            verifyNoInteractions(apiRateLimitRepository)
        }
    }
}
