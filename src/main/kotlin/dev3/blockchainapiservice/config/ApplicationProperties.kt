@file:Suppress("MagicNumber")

package dev3.blockchainapiservice.config

import dev3.blockchainapiservice.util.ChainId
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration
import java.math.BigInteger
import java.nio.file.Path
import java.time.Duration

@Configuration
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "blockchain-api-service")
class ApplicationProperties {
    var chain: Map<ChainId, ChainProperties> = emptyMap()
    var infuraId: String = ""
}

@ConfigurationProperties(prefix = "blockchain-api-service.jwt")
class JwtProperties {
    lateinit var publicKey: String
}

@ConfigurationProperties(prefix = "blockchain-api-service.ipfs")
class IpfsProperties {
    var url = "https://api.pinata.cloud/"
    var apiKey = ""
    var secretApiKey = ""
}

@ConstructorBinding
class ChainProperties(
    var name: String,
    var rpcUrl: String,
    var infuraUrl: String?,
    var startBlockNumber: BigInteger?,
    var minBlockConfirmationsForCaching: BigInteger?,
    var latestBlockCacheDuration: Duration = Duration.ofSeconds(5L)
)

@ConfigurationProperties(prefix = "blockchain-api-service.create-payout-queue")
class PayoutQueueProperties {
    var polling: Long = 5_000L
    var initialDelay: Long = 15_000L
}

@ConfigurationProperties(prefix = "blockchain-api-service.contract-decorators")
class ContractDecoratorProperties {
    var contractsDirectory: Path? = null
    var interfacesDirectory: Path? = null
    var ignoredDirs: List<String> = listOf(".git")
    var fillChangePollInterval: Duration = Duration.ofMinutes(1L)
    var fileChangeQuietInterval: Duration = Duration.ofSeconds(30L)
}

@ConfigurationProperties(prefix = "blockchain-api-service.meta-pixel-properties")
class MetaPixelProperties {
    var accessToken: String? = null
    var pixelId: String? = null
}

@ConfigurationProperties(prefix = "blockchain-api-service.contract-manifest-service")
class ContractManifestServiceProperties {
    var baseUrl: String? = null
    var decompileContractPath = "/decompile-contract"
}

@ConfigurationProperties(prefix = "blockchain-api-service.api-rate")
class ApiRateProperties {
    var usagePeriodDuration: Duration = Duration.ofDays(30L)
    var freeTierWriteRequests: Long = 200L
    var freeTierReadRequests: Long = 500_000L
}

@ConfigurationProperties(prefix = "blockchain-api-service.stripe")
class StripeProperties {
    var publishableKey: String? = null
    var secretKey: String? = null
    var webhookSecret: String? = null
}
