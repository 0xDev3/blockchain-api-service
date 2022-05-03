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
    val chainPoa = ChainProperties()
    var infuraId: String = ""
}

class JwtProperties {
    lateinit var publicKey: String
}

class ChainProperties {
    var startBlockNumber: BigInteger? = null
}
