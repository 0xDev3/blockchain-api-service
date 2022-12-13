package dev3.blockchainapiservice.features.billing.service

import com.fasterxml.jackson.databind.JsonNode
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
import com.stripe.param.InvoiceUpcomingParams
import com.stripe.param.PriceListParams
import com.stripe.param.ProductListParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionListParams
import com.stripe.param.SubscriptionUpdateParams
import dev3.blockchainapiservice.config.StripeProperties
import dev3.blockchainapiservice.exception.CustomerAlreadyExistsException
import dev3.blockchainapiservice.exception.CustomerCreationFailed
import dev3.blockchainapiservice.exception.CustomerNotYetCreatedException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.exception.WebhookException
import dev3.blockchainapiservice.features.billing.model.request.CreateSubscriptionRequest
import dev3.blockchainapiservice.features.billing.model.request.UpdateSubscriptionRequest
import dev3.blockchainapiservice.features.billing.model.response.AvailableSubscriptionResponse
import dev3.blockchainapiservice.features.billing.model.response.IntervalType
import dev3.blockchainapiservice.features.billing.model.response.SubscriptionPriceResponse
import dev3.blockchainapiservice.features.billing.model.response.SubscriptionResponse
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.util.UtcDateTime
import mu.KLogging
import org.springframework.stereotype.Service
import java.math.BigInteger

@Service
class StripeBillingServiceImpl( // TODO refactor and test
    private val userIdentifierRepository: UserIdentifierRepository,
    private val stripeProperties: StripeProperties,
    private val objectMapper: ObjectMapper
) : StripeBillingService {

    companion object : KLogging() {
        private val SUPPORTED_INTERVALS = listOf(IntervalType.MONTH, IntervalType.YEAR)
        private val NON_DIGIT_REGEX = "[^0-9]".toRegex()
        private const val READ_REQUESTS_KEY = "read_requests"
        private const val WRITE_REQUESTS_KEY = "write_requests"
    }

    override fun listAvailableSubscriptions(currency: String): List<AvailableSubscriptionResponse> {
        logger.debug { "Fetch available subscriptions" }

        val productParams = ProductListParams.builder()
            .setActive(true)
            .build()

        val products = Product.list(productParams).data.mapNotNull {
            it.mapMetadataBigIntValue(READ_REQUESTS_KEY) { readRequests ->
                it.mapMetadataBigIntValue(WRITE_REQUESTS_KEY) { writeRequests ->
                    AvailableSubscriptionResponse(
                        id = it.id,
                        name = it.name,
                        description = it.description ?: "",
                        readRequests = readRequests,
                        writeRequests = writeRequests,
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
                currentPeriodStart = UtcDateTime.ofEpochSeconds(subscription.currentPeriodStart).value,
                currentPeriodEnd = UtcDateTime.ofEpochSeconds(subscription.currentPeriodEnd).value,
                stripeSubscriptionData = objectMapper.readTree(subscription.toJson())
            )
        }
    }

    override fun createSubscription(
        requestBody: CreateSubscriptionRequest,
        userIdentifier: UserIdentifier
    ): SubscriptionResponse {
        logger.info { "Create subscription for user: $userIdentifier, params: $requestBody" }

        if (userIdentifier.stripeClientId == null) {
            throw CustomerNotYetCreatedException()
        }

        val params = SubscriptionCreateParams.builder()
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

        val subscription = Subscription.create(params)
        val stripeSubscriptionJson = objectMapper.readTree(subscription.toJson())

        return SubscriptionResponse(
            id = subscription.id,
            currentPeriodStart = UtcDateTime.ofEpochSeconds(subscription.currentPeriodStart).value,
            currentPeriodEnd = UtcDateTime.ofEpochSeconds(subscription.currentPeriodEnd).value,
            stripeSubscriptionData = stripeSubscriptionJson
        )
    }

    override fun updateSubscription(
        subscriptionId: String,
        requestBody: UpdateSubscriptionRequest,
        userIdentifier: UserIdentifier
    ): SubscriptionResponse {
        logger.info {
            "Update subscription for user: $userIdentifier, subscriptionId: $subscriptionId, params: $requestBody"
        }

        if (userIdentifier.stripeClientId == null) {
            throw CustomerNotYetCreatedException()
        }

        val subscription = Subscription.retrieve(subscriptionId)
            ?.takeIf { it.customer == userIdentifier.stripeClientId }
            ?: throw ResourceNotFoundException(
                "Subscription with id: $subscriptionId not found for user: ${userIdentifier.userIdentifier}"
            )

        val params = SubscriptionUpdateParams.builder()
            .addItem(
                SubscriptionUpdateParams.Item.builder()
                    .setId(subscription.items.data[0].id)
                    .setPrice(requestBody.newPriceId)
                    .build()
            )
            .setCancelAtPeriodEnd(false)
            .build()

        val updatedSubscription = subscription.update(params)

        return SubscriptionResponse(
            id = updatedSubscription.id,
            currentPeriodStart = UtcDateTime.ofEpochSeconds(updatedSubscription.currentPeriodStart).value,
            currentPeriodEnd = UtcDateTime.ofEpochSeconds(updatedSubscription.currentPeriodEnd).value,
            stripeSubscriptionData = objectMapper.readTree(updatedSubscription.toJson())
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

    override fun getInvoicePreview(subscriptionId: String, priceId: String, userIdentifier: UserIdentifier): JsonNode {
        logger.debug {
            "Get invoice preview for user: $userIdentifier, subscriptionId: $subscriptionId, priceId: $priceId"
        }

        if (userIdentifier.stripeClientId == null) {
            throw CustomerNotYetCreatedException()
        }

        val subscription = Subscription.retrieve(subscriptionId)
            ?.takeIf { it.customer == userIdentifier.stripeClientId }
            ?: throw ResourceNotFoundException(
                "Subscription with id: $subscriptionId not found for user: ${userIdentifier.userIdentifier}"
            )

        val params = InvoiceUpcomingParams.builder()
            .setCustomer(userIdentifier.stripeClientId)
            .setSubscription(subscriptionId)
            .addSubscriptionItem(
                InvoiceUpcomingParams
                    .SubscriptionItem.builder()
                    .setId(subscription.items.data[0].id)
                    .setPrice(priceId)
                    .build()
            )
            .build()

        val invoice = Invoice.upcoming(params)

        return objectMapper.readTree(invoice.toJson())
    }

    override fun webhook(payload: String, stripeSignature: String) {
        val event = try {
            Webhook.constructEvent(payload, stripeSignature, stripeProperties.webhookSecret)
        } catch (e: SignatureVerificationException) {
            logger.error { "Unable to process webhook event, payload: $payload, stripeSignature: $stripeSignature" }
            throw WebhookException()
        }

        val stripeObject = event.dataObjectDeserializer.`object`.orElse(null)

        logger.debug { "[TRACK] Got webhook event: ${event.type}, json: ${stripeObject?.toJson()}".replace('\n', ' ') }

        when (event.type) {
            // TODO
            //  "customer.subscription.updated" -> {
            //      val subscription = stripeObject as? Subscription ?: throw WebhookException()
            //  }

            "invoice.paid" -> {
                val invoice = stripeObject as? Invoice ?: throw WebhookException()
                val subscription = Subscription.retrieve(invoice.subscription) // ?.takeIf { it.status == "active" }

                logger.debug { "[TRACK] Subscription event: ${subscription.toJson()}" }

                if (subscription.status == "active") {
                    val start = UtcDateTime.ofEpochSeconds(subscription.currentPeriodStart)
                    val end = UtcDateTime.ofEpochSeconds(subscription.currentPeriodEnd)
                }
            }

            else -> {
            }
        }
    }

    private fun <T> Product.mapMetadataBigIntValue(key: String, mapping: (BigInteger) -> T): T? {
        val result = this.metadata[key]?.replace(NON_DIGIT_REGEX, "")?.toBigIntegerOrNull()?.let(mapping)

        if (result == null) {
            logger.warn { "Metadata key $key is not set correctly for product with id: $id, name: $name, skipping" }
        }

        return result
    }
}
