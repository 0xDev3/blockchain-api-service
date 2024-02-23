package dev3.blockchainapiservice.blockchain.properties

import dev3.blockchainapiservice.config.ApplicationProperties
import dev3.blockchainapiservice.config.ChainProperties
import dev3.blockchainapiservice.exception.UnsupportedChainIdException
import dev3.blockchainapiservice.util.ChainId
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class ChainPropertiesHandler(private val applicationProperties: ApplicationProperties) {

    private val blockchainPropertiesMap = ConcurrentHashMap<ChainId, ChainPropertiesWithServices>()

    fun getBlockchainProperties(chainSpec: ChainSpec): ChainPropertiesWithServices {
        val chainProperties = applicationProperties.chain[chainSpec.chainId]

        return if (chainSpec.customRpcUrl != null) {
            ChainPropertiesWithServices(
                web3j = Web3j.build(HttpService(chainSpec.customRpcUrl)),
                latestBlockCacheDuration = chainProperties?.latestBlockCacheDuration ?: Duration.ZERO,
                minBlockConfirmationsForCaching = chainProperties?.minBlockConfirmationsForCaching,
                fallbackChainIdForGasEstimate = chainProperties?.fallbackChainIdForGasEstimate?.let(::ChainId),
                safeGasEstimate = chainProperties?.safeGasEstimate
            )
        } else if (chainProperties != null) {
            blockchainPropertiesMap.computeIfAbsent(chainSpec.chainId) {
                generateBlockchainProperties(chainProperties)
            }
        } else {
            throw UnsupportedChainIdException(chainSpec.chainId)
        }
    }

    internal fun getChainRpcUrl(chainProperties: ChainProperties): String =
        if (chainProperties.infuraUrl == null || applicationProperties.infuraId.isBlank()) {
            chainProperties.rpcUrl
        } else {
            "${chainProperties.infuraUrl}${applicationProperties.infuraId}"
        }

    private fun generateBlockchainProperties(chainProperties: ChainProperties): ChainPropertiesWithServices {
        val rpcUrl = getChainRpcUrl(chainProperties)
        return ChainPropertiesWithServices(
            web3j = Web3j.build(HttpService(rpcUrl)),
            latestBlockCacheDuration = chainProperties.latestBlockCacheDuration,
            minBlockConfirmationsForCaching = chainProperties.minBlockConfirmationsForCaching,
            fallbackChainIdForGasEstimate = chainProperties.fallbackChainIdForGasEstimate?.let(::ChainId),
            safeGasEstimate = chainProperties.safeGasEstimate
        )
    }
}
