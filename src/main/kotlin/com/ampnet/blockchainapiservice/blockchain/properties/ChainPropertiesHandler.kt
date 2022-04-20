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
        val chain = Chain.fromId(chainSpec.chainId)

        return if (chainSpec.rpcSpec.urlOverride != null) {
            ChainPropertiesWithServices(
                web3j = Web3j.build(HttpService(chainSpec.rpcSpec.urlOverride))
            )
        } else if (chain != null) {
            blockchainPropertiesMap.computeIfAbsent(chain.id) {
                generateBlockchainProperties(chain)
            }
        } else if (chainSpec.rpcSpec.url != null) {
            ChainPropertiesWithServices(
                web3j = Web3j.build(HttpService(chainSpec.rpcSpec.url))
            )
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
}
