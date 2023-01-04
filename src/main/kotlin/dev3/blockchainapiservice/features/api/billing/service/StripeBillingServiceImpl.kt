package dev3.blockchainapiservice.features.api.billing.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.stripe.exception.SignatureVerificationException
import com.stripe.exception.StripeException
import com.stripe.model.Customer
import com.stripe.model.Invoice
import com.stripe.model.Price
import com.stripe.model.Product
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import com.stripe.param.CustomerCreateParams
import com.stripe.param.PriceListParams
import com.stripe.param.ProductListParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionListParams
import dev3.blockchainapiservice.config.StripeProperties
import dev3.blockchainapiservice.exception.CustomerAlreadyExistsException
import dev3.blockchainapiservice.exception.CustomerCreationFailed
import dev3.blockchainapiservice.exception.CustomerNotYetCreatedException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.exception.SubscriptionAlreadyActiveException
import dev3.blockchainapiservice.exception.WebhookException
import dev3.blockchainapiservice.features.api.billing.model.request.CreateSubscriptionRequest
import dev3.blockchainapiservice.features.api.billing.model.response.AvailableSubscriptionResponse
import dev3.blockchainapiservice.features.api.billing.model.response.IntervalType
import dev3.blockchainapiservice.features.api.billing.model.response.PayableSubscriptionResponse
import dev3.blockchainapiservice.features.api.billing.model.response.SubscriptionPriceResponse
import dev3.blockchainapiservice.features.api.billing.model.response.SubscriptionResponse
import dev3.blockchainapiservice.features.api.usage.model.result.ApiUsageLimit
import dev3.blockchainapiservice.features.api.usage.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.UtcDateTime
import mu.KLogging
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.days

@Service
@Suppress("TooManyFunctions")
class StripeBillingServiceImpl( // TODO test
    private val userIdentifierRepository: UserIdentifierRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val stripeProperties: StripeProperties,
    private val objectMapper: ObjectMapper
) : StripeBillingService {

    companion object : KLogging() {
        private val SUPPORTED_INTERVALS = listOf(IntervalType.MONTH, IntervalType.YEAR)
        private val NON_DIGIT_REGEX = "[^0-9]".toRegex()
        private val SUBSCRIPTION_PERIOD_DURATION = 30.days
        private const val READ_REQUESTS_KEY = "read_requests"
        private const val WRITE_REQUESTS_KEY = "write_requests"
    }

    override fun listAvailableSubscriptions(currency: String): List<AvailableSubscriptionResponse> {
        logger.debug { "Fetch available subscriptions" }

        val productParams = ProductListParams.builder()
            .setActive(true)
            .build()

        val products = Product.list(productParams).data.mapNotNull {
            it.mapMetadataLongValue(READ_REQUESTS_KEY) { readRequests ->
                it.mapMetadataLongValue(WRITE_REQUESTS_KEY) { writeRequests ->
                    AvailableSubscriptionResponse(
                        id = it.id,
                        name = it.name,
                        description = it.description ?: "",
                        readRequests = readRequests.toBigInteger(),
                        writeRequests = writeRequests.toBigInteger(),
                        prices = emptyList()
                    )
                }
            }
        }

        val priceParams = PriceListParams.builder()
            .setActive(true)
            .setCurrency(currency)
            .build()
        val prices = Price.list(priceParams).data

        val pricesByProduct = prices.groupBy { it.product }.mapValues { entry ->
            entry.value.map {
                SubscriptionPriceResponse(
                    id = it.id,
                    currency = it.currency,
                    amount = it.unitAmountDecimal,
                    intervalType = IntervalType.valueOf(it.recurring.interval.uppercase()),
                    intervalDuration = it.recurring.intervalCount
                )
            }.filter { it.intervalType in SUPPORTED_INTERVALS }
        }
        val productsWithPrices = products.map {
            it.copy(prices = pricesByProduct[it.id].orEmpty())
        }

        return productsWithPrices
    }

    override fun createCustomer(email: String, userIdentifier: UserIdentifier) {
        logger.info { "Creating Stripe customer for user: $userIdentifier, email: $email" }

        if (userIdentifier.stripeClientId != null) {
            throw CustomerAlreadyExistsException()
        }

        val params = CustomerCreateParams.builder()
            .setEmail(email)
            .build()

        val createdCustomer = try {
            Customer.create(params)
        } catch (e: StripeException) {
            logger.error(e) { "Customer creation failed for email: $email" }
            null
        }

        createdCustomer?.id?.let { userIdentifierRepository.setStripeClientId(userIdentifier.id, it) }
            ?: throw CustomerCreationFailed(email)
    }

    override fun hasActiveSubscription(userIdentifier: UserIdentifier): Boolean {
        logger.info { "Checking if user has active subscription: $userIdentifier" }

        return if (userIdentifier.stripeClientId == null) {
            false
        } else {
            val params = SubscriptionListParams.builder()
                .setStatus(SubscriptionListParams.Status.ACTIVE)
                .setCustomer(userIdentifier.stripeClientId)
                .build()
            Subscription.list(params).data.isNotEmpty()
        }
    }

    override fun getSubscriptions(userIdentifier: UserIdentifier): List<SubscriptionResponse> {
        logger.debug { "Get subscriptions for user: $userIdentifier" }

        if (userIdentifier.stripeClientId == null) {
            throw CustomerNotYetCreatedException()
        }

        val params = SubscriptionListParams.builder()
            .setStatus(SubscriptionListParams.Status.ALL)
            .setCustomer(userIdentifier.stripeClientId)
            .addAllExpand(mutableListOf("data.default_payment_method"))
            .build()

        val subscriptions = Subscription.list(params)

        return subscriptions.data.mapNotNull { subscription ->
            SubscriptionResponse(
                id = subscription.id,
                isActive = subscription.isActive(),
                currentPeriodStart = UtcDateTime.ofEpochSeconds(subscription.currentPeriodStart).value,
                currentPeriodEnd = UtcDateTime.ofEpochSeconds(subscription.currentPeriodEnd).value,
                stripeSubscriptionData = objectMapper.readTree(subscription.toJson())
            )
        }
    }

    override fun createSubscription(
        requestBody: CreateSubscriptionRequest,
        userIdentifier: UserIdentifier
    ): PayableSubscriptionResponse {
        logger.info { "Create subscription for user: $userIdentifier, params: $requestBody" }

        if (userIdentifier.stripeClientId == null) {
            throw CustomerNotYetCreatedException()
        }

        if (hasActiveSubscription(userIdentifier)) {
            throw SubscriptionAlreadyActiveException()
        }

        val createParams = SubscriptionCreateParams.builder()
            .setCustomer(userIdentifier.stripeClientId)
            .addItem(
                SubscriptionCreateParams.Item
                    .builder()
                    .setPrice(requestBody.priceId)
                    .build()
            )
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .addAllExpand(listOf("latest_invoice.payment_intent"))
            .build()

        val subscription = Subscription.create(createParams)
        val stripeSubscriptionJson = objectMapper.readTree(subscription.toJson())

        return PayableSubscriptionResponse(
            id = subscription.id,
            isActive = subscription.isActive(),
            stripePublishableKey = stripeProperties.publishableKey,
            currentPeriodStart = UtcDateTime.ofEpochSeconds(subscription.currentPeriodStart).value,
            currentPeriodEnd = UtcDateTime.ofEpochSeconds(subscription.currentPeriodEnd).value,
            stripeSubscriptionData = stripeSubscriptionJson
        )
    }

    override fun cancelSubscription(subscriptionId: String, userIdentifier: UserIdentifier) {
        logger.info { "Cancel subscription for user: $userIdentifier, subscriptionId: $subscriptionId" }

        if (userIdentifier.stripeClientId == null) {
            throw CustomerNotYetCreatedException()
        }

        val subscription = Subscription.retrieve(subscriptionId)
            ?.takeIf { it.customer == userIdentifier.stripeClientId }
            ?: throw ResourceNotFoundException(
                "Subscription with id: $subscriptionId not found for user: ${userIdentifier.userIdentifier}"
            )

        subscription.cancel()
    }

    override fun webhook(payload: String, stripeSignature: String) {
        val event = try {
            Webhook.constructEvent(payload, stripeSignature, stripeProperties.webhookSecret)
        } catch (e: SignatureVerificationException) {
            logger.error { "Unable to process webhook event, payload: $payload, stripeSignature: $stripeSignature" }
            throw WebhookException()
        }

        val stripeObject = event.dataObjectDeserializer.`object`.orElse(null)

        logger.debug { "Got webhook event: ${event.type}" }

        when (event.type) {
            "invoice.paid" -> {
                val invoice = stripeObject as? Invoice ?: throw WebhookException()
                val clientId = invoice.customer
                val userId = userIdentifierRepository.getByStripeClientId(clientId)?.id
                val subscription = Subscription.retrieve(invoice.subscription)

                if (userId != null && subscription.isActive()) {
                    subscription.activateNewSubscription(userId)
                } else {
                    logger.error {
                        "Active subscription or user does not exist for Stripe client ID," +
                            " subscription.status: ${subscription.status}, clientId: $clientId, userId: $userId"
                    }
                    throw WebhookException()
                }
            }

            else -> {
                logger.debug { "Unhandled webhook events: ${event.type}" }
            }
        }
    }

    private fun <T> Product.mapMetadataLongValue(key: String, mapping: (Long) -> T): T? {
        val result = this.metadata[key]?.replace(NON_DIGIT_REGEX, "")?.toLongOrNull()?.let(mapping)

        if (result == null) {
            logger.warn { "Metadata key $key is not set correctly for product with id: $id, name: $name, skipping" }
        }

        return result
    }

    private fun Subscription.isActive() = status == "active"

    private fun Subscription.activateNewSubscription(userId: UserId) {
        val start = UtcDateTime.ofEpochSeconds(currentPeriodStart)
        val end = UtcDateTime.ofEpochSeconds(currentPeriodEnd)
        val data = this.items.data[0]
        val intervalDurationInMonths = data.plan.let {
            IntervalType.valueOf(it.interval.uppercase()).toMonths(it.intervalCount)
        }.toInt()
        val product = Product.retrieve(data.plan.product)
        val writeRequests = product.mapMetadataLongValue(WRITE_REQUESTS_KEY) { it }
            ?: throw WebhookException()
        val readRequests = product.mapMetadataLongValue(READ_REQUESTS_KEY) { it }
            ?: throw WebhookException()

        val intervals = (0 until intervalDurationInMonths)
            .map { start + SUBSCRIPTION_PERIOD_DURATION * it } + end
        val limits = intervals.zipWithNext()
            .map {
                ApiUsageLimit(
                    userId = userId,
                    allowedWriteRequests = writeRequests,
                    allowedReadRequests = readRequests,
                    startDate = it.first,
                    endDate = it.second
                )
            }

        apiRateLimitRepository.createNewFutureUsageLimits(
            userId = userId,
            currentTime = utcDateTimeProvider.getUtcDateTime(),
            limits = limits
        )
    }
}
