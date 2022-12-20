package dev3.blockchainapiservice.features.billing.config

import com.stripe.Stripe
import dev3.blockchainapiservice.config.StripeProperties
import mu.KLogging
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class StripeConfig(private val stripeProperties: StripeProperties) {

    companion object : KLogging()

    @PostConstruct
    fun configureStripe() {
        logger.info { "Configuring Stripe payment API..." }
        Stripe.apiKey = stripeProperties.secretKey
    }
}
