package dev3.blockchainapiservice.features.billing.config

import com.stripe.Stripe
import dev3.blockchainapiservice.config.ApplicationProperties
import mu.KLogging
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class StripeConfig(private val applicationProperties: ApplicationProperties) {

    companion object : KLogging()

    @PostConstruct
    fun configureStripe() {
        logger.info { "Configuring Stripe payment API..." }

        requireNotNull(applicationProperties.stripe.publishableKey) { "Stripe publishable key is not set!" }
        requireNotNull(applicationProperties.stripe.secretKey) { "Stripe secret key is not set!" }
        requireNotNull(applicationProperties.stripe.webhookSecret) { "Stripe webhook secret is not set!" }

        Stripe.apiKey = applicationProperties.stripe.secretKey
    }
}
