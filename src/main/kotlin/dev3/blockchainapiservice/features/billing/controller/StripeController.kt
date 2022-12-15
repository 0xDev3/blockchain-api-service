package dev3.blockchainapiservice.features.billing.controller

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.features.billing.model.request.CreateCustomerRequest
import dev3.blockchainapiservice.features.billing.model.request.CreateSubscriptionRequest
import dev3.blockchainapiservice.features.billing.model.request.UpdateSubscriptionRequest
import dev3.blockchainapiservice.features.billing.model.response.AvailableSubscriptionsResponse
import dev3.blockchainapiservice.features.billing.model.response.SubscriptionResponse
import dev3.blockchainapiservice.features.billing.model.response.SubscriptionsResponse
import dev3.blockchainapiservice.features.billing.service.StripeBillingService
import dev3.blockchainapiservice.model.result.UserIdentifier
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class StripeController(private val billingService: StripeBillingService) { // TODO test

    @GetMapping("/v1/billing/available-plans")
    fun listAvailableSubscriptions(
        @RequestParam("currency", required = false, defaultValue = "USD") currency: String
    ): ResponseEntity<AvailableSubscriptionsResponse> =
        ResponseEntity.ok(
            AvailableSubscriptionsResponse(
                billingService.listAvailableSubscriptions(currency)
            )
        )

    @PostMapping("/v1/billing/customer")
    fun createCustomer(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateCustomerRequest
    ) {
        billingService.createCustomer(requestBody.email, userIdentifier)
    }

    @GetMapping("/v1/billing/subscriptions")
    fun getSubscriptions(
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<SubscriptionsResponse> =
        ResponseEntity.ok(
            SubscriptionsResponse(
                billingService.getSubscriptions(userIdentifier)
            )
        )

    @PostMapping("/v1/billing/subscriptions")
    fun createSubscription(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateSubscriptionRequest
    ): ResponseEntity<SubscriptionResponse> =
        ResponseEntity.ok(
            billingService.createSubscription(requestBody, userIdentifier)
        )

    @PatchMapping("/v1/billing/subscriptions/{id}")
    fun updateSubscription(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable("id") id: String,
        @Valid @RequestBody requestBody: UpdateSubscriptionRequest
    ): ResponseEntity<SubscriptionResponse> =
        ResponseEntity.ok(
            billingService.updateSubscription(id, requestBody, userIdentifier)
        )

    @DeleteMapping("/v1/billing/subscriptions/{id}")
    fun cancelSubscription(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable("id") id: String
    ) {
        billingService.cancelSubscription(id, userIdentifier)
    }

    @GetMapping("/v1/billing/subscriptions/{subscriptionId}/invoice-preview/{priceId}")
    fun previewInvoice(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable("subscriptionId") subscriptionId: String,
        @PathVariable("priceId") priceId: String
    ): ResponseEntity<JsonNode> =
        ResponseEntity.ok(
            billingService.getInvoicePreview(subscriptionId, priceId, userIdentifier)
        )

    @PostMapping("/v1/billing/webhook")
    fun webhook(
        @RequestHeader("Stripe-Signature") stripeSignature: String,
        @RequestBody requestBody: String
    ) {
        billingService.webhook(requestBody, stripeSignature)
    }
}