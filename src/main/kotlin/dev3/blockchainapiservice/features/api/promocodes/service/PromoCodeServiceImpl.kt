package dev3.blockchainapiservice.features.api.promocodes.service

import dev3.blockchainapiservice.config.AdminProperties
import dev3.blockchainapiservice.exception.AccessForbiddenException
import dev3.blockchainapiservice.exception.PromoCodeAlreadyUsedException
import dev3.blockchainapiservice.exception.PromoCodeExpiredException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.exception.SubscriptionAlreadyActiveException
import dev3.blockchainapiservice.features.api.billing.service.StripeBillingService
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeAlreadyUsed
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeDoesNotExist
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeExpired
import dev3.blockchainapiservice.features.api.promocodes.repository.PromoCodeRepository
import dev3.blockchainapiservice.features.api.usage.model.result.ApiUsageLimit
import dev3.blockchainapiservice.features.api.usage.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.service.RandomProvider
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.UtcDateTime
import mu.KLogging
import org.springframework.stereotype.Service
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.days

@Service
class PromoCodeServiceImpl(
    private val billingService: StripeBillingService,
    private val promoCodeRepository: PromoCodeRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val randomProvider: RandomProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val adminProperties: AdminProperties
) : PromoCodeService {

    companion object : KLogging() {
        private const val PROMO_CODE_LENGTH = 6
        private val PROMO_CODE_CHARS = ('A'..'Z').toList() + ('0'..'9')
        private val PROMO_PERIOD_DURATION = 30.days
    }

    override fun generatePromoCode(
        userIdentifier: UserIdentifier,
        prefix: String,
        writeRequests: Long,
        readRequests: Long,
        validUntil: UtcDateTime
    ): PromoCode {
        logger.info {
            "Generate promo code, userIdentifier: $userIdentifier, prefix: $prefix, writeRequests: $writeRequests," +
                " readRequests: $readRequests, validUntil: $validUntil"
        }

        userIdentifier.checkIfAllowed("Current user is not allowed to generate promo code.")

        val generatedCode = randomProvider.getBytes(PROMO_CODE_LENGTH)
            .joinToString("") { PROMO_CODE_CHARS[it.toInt().absoluteValue % PROMO_CODE_CHARS.size].toString() }
        val code = "$prefix$generatedCode"

        return promoCodeRepository.storeCode(
            code = code,
            writeRequests = writeRequests,
            readRequests = readRequests,
            validUntil = validUntil
        )
    }

    override fun getPromoCodes(
        userIdentifier: UserIdentifier,
        validFrom: UtcDateTime,
        validUntil: UtcDateTime
    ): List<PromoCode> {
        logger.debug {
            "Get promo codes, userIdentifier: $userIdentifier, validFrom: $validFrom, validUntil: $validUntil"
        }

        userIdentifier.checkIfAllowed("Current user is not allowed to fetch promo codes.")

        return promoCodeRepository.getCodes(validFrom = validFrom, validUntil = validUntil)
    }

    @Suppress("ThrowsCount")
    override fun usePromoCode(userIdentifier: UserIdentifier, code: String) {
        logger.info { "Use promo code for userIdentifier: $userIdentifier, code: $code" }

        if (billingService.hasActiveSubscription(userIdentifier)) {
            throw SubscriptionAlreadyActiveException()
        }

        val currentTime = utcDateTimeProvider.getUtcDateTime()
        val promoCodeResult = promoCodeRepository.useCode(
            code = code,
            userId = userIdentifier.id,
            currentTime = currentTime
        )

        val promoCode = when (promoCodeResult) {
            is PromoCode -> promoCodeResult
            is PromoCodeExpired -> throw PromoCodeExpiredException(code)
            is PromoCodeDoesNotExist -> throw ResourceNotFoundException("Expired or non-existent promo code: $code")
            is PromoCodeAlreadyUsed -> throw PromoCodeAlreadyUsedException(code)
        }

        val promoLimit = ApiUsageLimit(
            userId = userIdentifier.id,
            allowedWriteRequests = promoCode.writeRequests,
            allowedReadRequests = promoCode.readRequests,
            startDate = currentTime,
            endDate = currentTime + PROMO_PERIOD_DURATION
        )

        apiRateLimitRepository.createNewFutureUsageLimits(
            userId = userIdentifier.id,
            currentTime = utcDateTimeProvider.getUtcDateTime(),
            limits = listOf(promoLimit)
        )
    }

    private fun UserIdentifier.checkIfAllowed(message: String) {
        if (this is UserWalletAddressIdentifier && walletAddress !in adminProperties.wallets) {
            throw AccessForbiddenException(message)
        }
    }
}
