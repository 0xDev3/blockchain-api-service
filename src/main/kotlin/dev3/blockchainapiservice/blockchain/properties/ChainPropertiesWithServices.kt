package dev3.blockchainapiservice.blockchain.properties

import dev3.blockchainapiservice.util.ChainId
import org.web3j.protocol.Web3j
import java.math.BigInteger
import java.time.Duration

data class ChainPropertiesWithServices(
    val web3j: Web3j,
    val latestBlockCacheDuration: Duration,
    val minBlockConfirmationsForCaching: BigInteger?,
    val fallbackChainIdForGasEstimate: ChainId?,
    val safeGasEstimate: BigInteger?
) {
    fun shouldCache(blockConfirmations: BigInteger): Boolean =
        minBlockConfirmationsForCaching != null && blockConfirmations >= minBlockConfirmationsForCaching
}
