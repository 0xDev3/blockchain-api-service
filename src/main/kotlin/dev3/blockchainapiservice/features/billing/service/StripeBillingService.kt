package dev3.blockchainapiservice.features.billing.service

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.features.billing.model.request.CreateSubscriptionRequest
import dev3.blockchainapiservice.features.billing.model.request.UpdateSubscriptionRequest
import dev3.blockchainapiservice.features.billing.model.response.AvailableSubscriptionResponse
import dev3.blockchainapiservice.features.billing.model.response.SubscriptionResponse
import dev3.blockchainapiservice.model.result.UserIdentifier

interface StripeBillingService {
    fun listAvailableSubscriptions(currency: String): List<AvailableSubscriptionResponse>
    fun createCustomer(email: String, userIdentifier: UserIdentifier)
    fun getSubscriptions(userIdentifier: UserIdentifier): List<SubscriptionResponse>
    fun createSubscription(requestBody: CreateSubscriptionRequest, userIdentifier: UserIdentifier): SubscriptionResponse
    fun updateSubscription(
        subscriptionId: String,
        requestBody: UpdateSubscriptionRequest,
        userIdentifier: UserIdentifier
    ): SubscriptionResponse

    fun cancelSubscription(subscriptionId: String, userIdentifier: UserIdentifier)
    fun getInvoicePreview(subscriptionId: String, priceId: String, userIdentifier: UserIdentifier): JsonNode
    fun webhook(payload: String, stripeSignature: String)
}
