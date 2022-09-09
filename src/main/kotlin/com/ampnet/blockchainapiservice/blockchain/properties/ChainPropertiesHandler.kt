package com.ampnet.blockchainapiservice.blockchain.properties

import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.config.ChainProperties
import com.ampnet.blockchainapiservice.exception.UnsupportedChainIdException
import com.ampnet.blockchainapiservice.util.ChainId
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.util.concurrent.ConcurrentHashMap

class ChainPropertiesHandler(private val applicationProperties: ApplicationProperties) {

    private val blockchainPropertiesMap = ConcurrentHashMap<ChainId, ChainPropertiesWithServices>()

    fun getBlockchainProperties(chainSpec: ChainSpec): ChainPropertiesWithServices {
        val chain = Chain.fromId(chainSpec.chainId)

        return if (chainSpec.customRpcUrl != null) {
            ChainPropertiesWithServices(
                web3j = Web3j.build(HttpService(chainSpec.customRpcUrl))
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
        return when (chainId) {
            Chain.MATIC_MAIN.id -> applicationProperties.chainMatic
            Chain.MATIC_TESTNET_MUMBAI.id -> applicationProperties.chainMumbai
            Chain.ETHEREUM_MAIN.id -> applicationProperties.chainEthereum
            Chain.GOERLI_TESTNET.id -> applicationProperties.chainGoerli
            Chain.HARDHAT_TESTNET.id -> applicationProperties.chainHardhatTestnet
            Chain.BSC.id -> applicationProperties.chainBsc
            Chain.XDAI.id -> applicationProperties.chainXdai
            Chain.FANTOM.id -> applicationProperties.chainFantom
            Chain.MOONRIVER.id -> applicationProperties.chainMoonriver
            Chain.AVAX.id -> applicationProperties.chainAvalanche
            Chain.AURORA.id -> applicationProperties.chainAurora
            else -> null
        }
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
        return ChainPropertiesWithServices(
            web3j = Web3j.build(HttpService(rpcUrl))
        )
    }
}
