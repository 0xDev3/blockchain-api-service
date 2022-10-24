package com.ampnet.blockchainapiservice.blockchain.properties

import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.config.ChainProperties
import com.ampnet.blockchainapiservice.exception.UnsupportedChainIdException
import com.ampnet.blockchainapiservice.util.ChainId
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class ChainPropertiesHandler(private val applicationProperties: ApplicationProperties) {

    private val blockchainPropertiesMap = ConcurrentHashMap<ChainId, ChainPropertiesWithServices>()

    fun getBlockchainProperties(chainSpec: ChainSpec): ChainPropertiesWithServices {
        val chain = Chain.fromId(chainSpec.chainId)

        return if (chainSpec.customRpcUrl != null) {
            val chainProperties = chain?.let { getChainProperties(it.id) }
            ChainPropertiesWithServices(
                web3j = Web3j.build(HttpService(chainSpec.customRpcUrl)),
                latestBlockCacheDuration = chainProperties?.latestBlockCacheDuration ?: Duration.ZERO,
                minBlockConfirmationsForCaching = chainProperties?.minBlockConfirmationsForCaching
            )
        } else if (chain != null) {
            blockchainPropertiesMap.computeIfAbsent(chain.id) {
                generateBlockchainProperties(chain)
            }
        } else {
            throw UnsupportedChainIdException(chainSpec.chainId)
        }
    }

    fun getChainProperties(chainId: ChainId): ChainProperties? {
        return Chain.fromId(chainId)?.propertiesProvider?.invoke(applicationProperties)
    }

    internal fun getChainRpcUrl(chain: Chain): String =
        getChainProperties(chain.id)?.rpcUrlOverride
            ?: if (chain.infura == null || applicationProperties.infuraId.isBlank()) {
                chain.rpcUrl
            } else {
                "${chain.infura}${applicationProperties.infuraId}"
            }

    private fun generateBlockchainProperties(chain: Chain): ChainPropertiesWithServices {
        val rpcUrl = getChainRpcUrl(chain)
        val chainProperties = getChainProperties(chain.id)
        return ChainPropertiesWithServices(
            web3j = Web3j.build(HttpService(rpcUrl)),
            latestBlockCacheDuration = chainProperties?.latestBlockCacheDuration ?: Duration.ZERO,
            minBlockConfirmationsForCaching = chainProperties?.minBlockConfirmationsForCaching
        )
    }
}
