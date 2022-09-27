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

    companion object {
        private val CHAIN_PROPERTIES_RESOLVER_MAP = mapOf<ChainId, (ApplicationProperties) -> ChainProperties>(
            Chain.MATIC_MAIN.id to { it.chainMatic },
            Chain.MATIC_TESTNET_MUMBAI.id to { it.chainMumbai },
            Chain.ETHEREUM_MAIN.id to { it.chainEthereum },
            Chain.GOERLI_TESTNET.id to { it.chainGoerli },
            Chain.HARDHAT_TESTNET.id to { it.chainHardhatTestnet },
            Chain.BSC.id to { it.chainBsc },
            Chain.XDAI.id to { it.chainXdai },
            Chain.FANTOM.id to { it.chainFantom },
            Chain.MOONRIVER.id to { it.chainMoonriver },
            Chain.AVAX.id to { it.chainAvalanche },
            Chain.AURORA.id to { it.chainAurora },
            Chain.ARBITRUM.id to { it.chainArbitrum },
            Chain.OPTIMISM.id to { it.chainOptimism },
            Chain.CELO.id to { it.chainCelo },
            Chain.PARATIME.id to { it.chainParaTime },
            Chain.MOONBEAM.id to { it.chainMoonbeam }
        )
    }

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
        return CHAIN_PROPERTIES_RESOLVER_MAP[chainId]?.invoke(applicationProperties)
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
