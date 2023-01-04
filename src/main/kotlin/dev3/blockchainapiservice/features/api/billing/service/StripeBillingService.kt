package dev3.blockchainapiservice.features.api.billing.service

import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.api.billing.model.request.CreateSubscriptionRequest
import dev3.blockchainapiservice.features.api.billing.model.response.AvailableSubscriptionResponse
import dev3.blockchainapiservice.features.api.billing.model.response.PayableSubscriptionResponse
import dev3.blockchainapiservice.features.api.billing.model.response.SubscriptionResponse

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
