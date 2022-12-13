@file:Suppress("MagicNumber")

package dev3.blockchainapiservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration
import java.math.BigInteger
import java.nio.file.Path
import java.time.Duration

@Configuration
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "blockchain-api-service")
class ApplicationProperties {
    val chainEthereum = ChainProperties()
    val chainGoerli = ChainProperties()
    val chainMatic = ChainProperties()
    val chainMumbai = ChainProperties()
    val chainHardhatTestnet = ChainProperties()
    val chainBsc = ChainProperties()
    val chainXdai = ChainProperties()
    val chainFantom = ChainProperties()
    val chainMoonriver = ChainProperties()
    val chainAvalanche = ChainProperties()
    val chainAurora = ChainProperties()
    val chainArbitrum = ChainProperties()
    val chainOptimism = ChainProperties()
    val chainCelo = ChainProperties()
    val chainParaTime = ChainProperties()
    val chainMoonbeam = ChainProperties()
    val chainPolygonZkEvmTestnet = ChainProperties()
    val chainCeloAlfajoresTestnet = ChainProperties()
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

class ChainProperties {
    var minBlockConfirmationsForCaching: BigInteger? = null
    var latestBlockCacheDuration: Duration = Duration.ofSeconds(5L)
    var rpcUrlOverride: String? = null
    var startBlockNumber: BigInteger? = null
}

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
    var freeTierWriteRequests: Int = 200
    var freeTierReadRequests: Int = 500_000
}

@ConfigurationProperties(prefix = "blockchain-api-service.stripe")
class StripeProperties {
    var publishableKey: String? = null
    var secretKey: String? = null
    var webhookSecret: String? = null
}
