package dev3.blockchainapiservice.features.billing.service

import dev3.blockchainapiservice.features.billing.model.request.CreateSubscriptionRequest
import dev3.blockchainapiservice.features.billing.model.response.AvailableSubscriptionResponse
import dev3.blockchainapiservice.features.billing.model.response.PayableSubscriptionResponse
import dev3.blockchainapiservice.features.billing.model.response.SubscriptionResponse
import dev3.blockchainapiservice.model.result.UserIdentifier

interface StripeBillingService {
    fun listAvailableSubscriptions(currency: String): List<AvailableSubscriptionResponse>
    fun createCustomer(email: String, userIdentifier: UserIdentifier)
    fun hasActiveSubscription(userIdentifier: UserIdentifier): Boolean
    fun getSubscriptions(userIdentifier: UserIdentifier): List<SubscriptionResponse>
    fun createSubscription(
        requestBody: CreateSubscriptionRequest,
        userIdentifier: UserIdentifier
    ): PayableSubscriptionResponse

    fun cancelSubscription(subscriptionId: String, userIdentifier: UserIdentifier)
    fun webhook(payload: String, stripeSignature: String)
}
