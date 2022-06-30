package com.ampnet.blockchainapiservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.math.BigInteger

@Configuration
@ConfigurationProperties(prefix = "blockchain-api-service")
class ApplicationProperties {
    val jwt = JwtProperties()
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
    var infuraId: String = ""
}

class JwtProperties {
    lateinit var publicKey: String
}

class ChainProperties {
    var startBlockNumber: BigInteger? = null
}
