@file:Suppress("MagicNumber")

package dev3.blockchainapiservice.config

import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration
import java.math.BigInteger
import java.nio.file.Path
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Configuration
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "blockchain-api-service")
class ApplicationProperties {
    var chain: Map<ChainId, ChainProperties> = emptyMap()
    var infuraId: String = ""
}

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.jwt")
data class JwtProperties(
    val publicKey: String
)

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.ipfs")
data class IpfsProperties(
    val url: String = "https://api.pinata.cloud/",
    val apiKey: String = "",
    val secretApiKey: String = ""
)

@ConstructorBinding
data class ChainProperties(
    val name: String,
    val rpcUrl: String,
    val infuraUrl: String?,
    val startBlockNumber: BigInteger?,
    val minBlockConfirmationsForCaching: BigInteger?,
    val latestBlockCacheDuration: Duration = 5.seconds.toJavaDuration()
)

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.create-payout-queue")
data class PayoutQueueProperties(
    val polling: Long = 5_000L,
    val initialDelay: Long = 15_000L
)

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.contract-decorators")
data class ContractDecoratorProperties(
    val contractsDirectory: Path?,
    val interfacesDirectory: Path?,
    val ignoredDirs: List<String> = listOf(".git"),
    val fillChangePollInterval: Duration = 1.minutes.toJavaDuration(),
    val fileChangeQuietInterval: Duration = 30.seconds.toJavaDuration()
)

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.meta-pixel-properties")
data class MetaPixelProperties(
    val accessToken: String?,
    val pixelId: String?
)

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.contract-manifest-service")
data class ContractManifestServiceProperties(
    val baseUrl: String?,
    val decompileContractPath: String = "/decompile-contract"
)

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.api-rate")
data class ApiRateProperties(
    val usagePeriodDuration: Duration = 30.days.toJavaDuration(),
    val freeTierWriteRequests: Long = 200L,
    val freeTierReadRequests: Long = 500_000L
)

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.stripe")
data class StripeProperties(
    val publishableKey: String,
    val secretKey: String,
    val webhookSecret: String
)

@ConstructorBinding
@ConfigurationProperties(prefix = "blockchain-api-service.promo-code")
data class PromoCodeProperties(
    val allowedWallets: Set<WalletAddress> = emptySet()
)
