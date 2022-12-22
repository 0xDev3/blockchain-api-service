package dev3.blockchainapiservice.features.promocode.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.AdminProperties
import dev3.blockchainapiservice.exception.AccessForbiddenException
import dev3.blockchainapiservice.exception.PromoCodeAlreadyUsedException
import dev3.blockchainapiservice.exception.PromoCodeExpiredException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.exception.SubscriptionAlreadyActiveException
import dev3.blockchainapiservice.features.billing.service.StripeBillingService
import dev3.blockchainapiservice.features.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.promocodes.model.result.PromoCodeAlreadyUsed
import dev3.blockchainapiservice.features.promocodes.model.result.PromoCodeDoesNotExist
import dev3.blockchainapiservice.features.promocodes.model.result.PromoCodeExpired
import dev3.blockchainapiservice.features.promocodes.repository.PromoCodeRepository
import dev3.blockchainapiservice.features.promocodes.service.PromoCodeServiceImpl
import dev3.blockchainapiservice.model.result.ApiUsageLimit
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.service.RandomProvider
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.UUID
import kotlin.time.Duration.Companion.days

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
        val adminProperties = suppose("user is allowed to generate promo code") {
            AdminProperties(wallets = setOf(USER_IDENTIFIER.walletAddress))
        }

        val randomProvider = mock<RandomProvider>()

        suppose("some promo code will be generated") {
            call(randomProvider.getBytes(6))
                .willReturn(ByteArray(6) { it.toByte() })
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("promo code will be stored into the database") {
            call(
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
            adminProperties = adminProperties
        )

        verify("promo code is correctly generated and stored") {
            val result = service.generatePromoCode(
                userIdentifier = USER_IDENTIFIER,
                prefix = "DEV3-",
                writeRequests = PROMO_CODE.writeRequests,
                readRequests = PROMO_CODE.readRequests,
                validUntil = TestData.TIMESTAMP
            )

            expectThat(result)
                .isEqualTo(PROMO_CODE)
        }
    }

    @Test
    fun mustThrowAccessForbiddenExceptionWhenUserIsNotAllowedToGeneratePromoCode() {
        val adminProperties = suppose("user is not allowed to generate promo code") {
            AdminProperties()
        }

        val service = PromoCodeServiceImpl(
            billingService = mock(),
            promoCodeRepository = mock(),
            apiRateLimitRepository = mock(),
            randomProvider = mock(),
            utcDateTimeProvider = mock(),
            adminProperties = adminProperties
        )

        verify("AccessForbiddenException is thrown") {
            expectThrows<AccessForbiddenException> {
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
        val adminProperties = suppose("user is allowed to generate promo code") {
            AdminProperties(wallets = setOf(USER_IDENTIFIER.walletAddress))
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo codes will be returned") {
            call(promoCodeRepository.getCodes(TestData.TIMESTAMP, TestData.TIMESTAMP + 1.days))
                .willReturn(listOf(PROMO_CODE))
        }

        val service = PromoCodeServiceImpl(
            billingService = mock(),
            promoCodeRepository = promoCodeRepository,
            apiRateLimitRepository = mock(),
            randomProvider = mock(),
            utcDateTimeProvider = mock(),
            adminProperties = adminProperties
        )

        verify("promo codes are correctly fetched") {
            val result = service.getPromoCodes(
                userIdentifier = USER_IDENTIFIER,
                validFrom = TestData.TIMESTAMP,
                validUntil = TestData.TIMESTAMP + 1.days
            )

            expectThat(result)
                .isEqualTo(listOf(PROMO_CODE))
        }
    }

    @Test
    fun mustThrowAccessForbiddenExceptionWhenUserIsNotAllowedToFetchPromoCodes() {
        val adminProperties = suppose("user is not allowed to generate promo code") {
            AdminProperties()
        }

        val service = PromoCodeServiceImpl(
            billingService = mock(),
            promoCodeRepository = mock(),
            apiRateLimitRepository = mock(),
            randomProvider = mock(),
            utcDateTimeProvider = mock(),
            adminProperties = adminProperties
        )

        verify("AccessForbiddenException is thrown") {
            expectThrows<AccessForbiddenException> {
                service.getPromoCodes(
                    userIdentifier = USER_IDENTIFIER,
                    validFrom = TestData.TIMESTAMP,
                    validUntil = TestData.TIMESTAMP + 1.days
                )
            }
        }
    }

    @Test
    fun mustAllowUserToUsePromoCode() {
        val billingService = mock<StripeBillingService>()

        suppose("user does not have an active subscription") {
            call(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(false)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo code is used") {
            call(
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
            adminProperties = AdminProperties()
        )

        verify("promo code is used and api rate limit is added to the user") {
            service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)

            expectInteractions(apiRateLimitRepository) {
                once.createNewFutureUsageLimits(
                    userId = USER_IDENTIFIER.id,
                    currentTime = TestData.TIMESTAMP,
                    limits = listOf(
                        ApiUsageLimit(
                            userId = USER_IDENTIFIER.id,
                            allowedWriteRequests = PROMO_CODE.writeRequests,
                            allowedReadRequests = PROMO_CODE.readRequests,
                            startDate = TestData.TIMESTAMP,
                            endDate = TestData.TIMESTAMP + 30.days
                        )
                    )
                )
            }
        }
    }

    @Test
    fun mustThrowSubscriptionAlreadyActiveExceptionWhenUserIsTryingToClaimPromoCodeAndHasActiveSubscription() {
        val billingService = mock<StripeBillingService>()

        suppose("user has an active subscription") {
            call(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(true)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val service = PromoCodeServiceImpl(
            billingService = billingService,
            promoCodeRepository = mock(),
            apiRateLimitRepository = apiRateLimitRepository,
            randomProvider = mock(),
            utcDateTimeProvider = mock(),
            adminProperties = AdminProperties()
        )

        verify("SubscriptionAlreadyActiveException is thrown") {
            expectThrows<SubscriptionAlreadyActiveException> {
                service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)
            }

            expectNoInteractions(apiRateLimitRepository)
        }
    }

    @Test
    fun mustThrowPromoCodeExpiredExceptionWhenPromoCodeHasExpired() {
        val billingService = mock<StripeBillingService>()

        suppose("user does not have an active subscription") {
            call(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(false)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo code has expired") {
            call(
                promoCodeRepository.useCode(
                    code = PROMO_CODE.code,
                    userId = USER_IDENTIFIER.id,
                    currentTime = TestData.TIMESTAMP
                )
            )
                .willReturn(PromoCodeExpired)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val service = PromoCodeServiceImpl(
            billingService = billingService,
            promoCodeRepository = promoCodeRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            randomProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider,
            adminProperties = AdminProperties()
        )

        verify("PromoCodeExpiredException is thrown") {
            expectThrows<PromoCodeExpiredException> {
                service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)
            }

            expectNoInteractions(apiRateLimitRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenPromoCodeDoesNotExist() {
        val billingService = mock<StripeBillingService>()

        suppose("user does not have an active subscription") {
            call(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(false)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo code does not exist") {
            call(
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
            adminProperties = AdminProperties()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)
            }

            expectNoInteractions(apiRateLimitRepository)
        }
    }

    @Test
    fun mustThrowPromoCodeAlreadyUsedExceptionWhenUserHasAlreadyUsedSomePromoCode() {
        val billingService = mock<StripeBillingService>()

        suppose("user does not have an active subscription") {
            call(billingService.hasActiveSubscription(USER_IDENTIFIER))
                .willReturn(false)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val promoCodeRepository = mock<PromoCodeRepository>()

        suppose("some promo code was already used") {
            call(
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
            adminProperties = AdminProperties()
        )

        verify("PromoCodeAlreadyUsedException is thrown") {
            expectThrows<PromoCodeAlreadyUsedException> {
                service.usePromoCode(USER_IDENTIFIER, PROMO_CODE.code)
            }

            expectNoInteractions(apiRateLimitRepository)
        }
    }
}
