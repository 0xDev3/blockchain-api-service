package com.ampnet.blockchainapiservice.blockchain.properties

import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.config.ChainProperties
import com.ampnet.blockchainapiservice.exception.UnsupportedChainIdException
import com.ampnet.blockchainapiservice.util.ChainId
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class ChainPropertiesHandler(private val applicationProperties: ApplicationProperties) {

    private val blockchainPropertiesMap = mutableMapOf<ChainId, ChainPropertiesWithServices>()

    fun getBlockchainProperties(chainSpec: ChainSpec): ChainPropertiesWithServices {
        return if (chainSpec.rpcUrl != null) {
            ChainPropertiesWithServices(
                web3j = Web3j.build(HttpService(chainSpec.rpcUrl))
            )
        } else {
            blockchainPropertiesMap.computeIfAbsent(chainSpec.chainId) {
                generateBlockchainProperties(getChain(it))
            }
        }
    }

    fun getChainProperties(chainId: ChainId): ChainProperties? {
        return when (chainId) {
            Chain.MATIC_MAIN.id -> applicationProperties.chainMatic
            Chain.MATIC_TESTNET_MUMBAI.id -> applicationProperties.chainMumbai
            Chain.ETHEREUM_MAIN.id -> applicationProperties.chainEthereum
            Chain.GOERLI_TESTNET.id -> applicationProperties.chainGoerli
            Chain.HARDHAT_TESTNET.id -> applicationProperties.chainHardhatTestnet
            Chain.AMPNET_POA.id -> applicationProperties.chainPoa
            else -> null
        }
    }

    internal fun getChainRpcUrl(chain: Chain): String =
        if (chain.infura == null || applicationProperties.infuraId.isBlank()) {
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

    private fun getChain(chainId: ChainId) = Chain.fromId(chainId)
        ?: throw UnsupportedChainIdException(chainId)
}
