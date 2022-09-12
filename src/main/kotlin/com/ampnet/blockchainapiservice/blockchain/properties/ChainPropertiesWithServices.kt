package com.ampnet.blockchainapiservice.blockchain.properties

import org.web3j.protocol.Web3j
import java.math.BigInteger

data class ChainPropertiesWithServices(
    val web3j: Web3j,
    val minBlockConfirmationsForCaching: BigInteger?
) {
    fun shouldCache(blockConfirmations: BigInteger): Boolean =
        minBlockConfirmationsForCaching != null && blockConfirmations >= minBlockConfirmationsForCaching
}
