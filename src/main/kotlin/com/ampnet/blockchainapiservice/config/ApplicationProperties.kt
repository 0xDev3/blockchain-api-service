package com.ampnet.blockchainapiservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.math.BigInteger
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "blockchain-api-service")
class ApplicationProperties {
    val jwt = JwtProperties()
    val chainEthereum = ChainProperties()
    val chainGoerli = ChainProperties()
    val chainMatic = ChainProperties()
    val chainMumbai = ChainProperties()
    val chainHardhatTestnet = ChainProperties()
    val verification = VerificationProperties()
    var infuraId: String = ""
}

class JwtProperties {
    lateinit var publicKey: String
}

class ChainProperties {
    var startBlockNumber: BigInteger? = null
}

@Suppress("MagicNumber")
class VerificationProperties {
    var unsignedMessageValidity: Duration = Duration.ofMinutes(15L)
    var signedMessageValidity: Duration = Duration.ofMinutes(5L)
}
