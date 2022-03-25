package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.exception.UnsupportedChainIdException
import com.ampnet.blockchainapiservice.util.ChainId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChainPropertiesHandlerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateChainPropertiesWithServicesWhenRpcUrlIsNull() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(ApplicationProperties().apply { infuraId = "" })
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(Chain.MATIC_TESTNET_MUMBAI.id.toSpec())
            assertThat(chainProperties.web3j).withMessage().isNotNull()
        }
    }

    @Test
    fun mustCorrectlyCreateChainPropertiesWithServicesWhenRpcUrlIsSpecified() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(ApplicationProperties().apply { infuraId = "" })
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(
                ChainSpec(
                    chainId = ChainId(123L),
                    rpcUrl = "http://localhost:1234/"
                )
            )
            assertThat(chainProperties.web3j).withMessage().isNotNull()
        }
    }

    @Test
    fun mustThrowExceptionForInvalidChainId() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(ApplicationProperties())
        }

        verify("InternalException is thrown") {
            val exception = assertThrows<UnsupportedChainIdException>(message) {
                chainPropertiesHandler.getBlockchainProperties(ChainId(-1).toSpec())
            }
            assertThat(exception.errorCode).withMessage().isEqualTo(ErrorCode.UNSUPPORTED_CHAIN_ID)
        }
    }

    @Test
    fun mustReturnDefaultRpcIfInfuraIdIsMissing() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            val applicationProperties = ApplicationProperties().apply { infuraId = "" }
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct RPC URL is returned") {
            val chain = Chain.MATIC_TESTNET_MUMBAI
            val rpc = chainPropertiesHandler.getChainRpcUrl(chain)
            assertThat(rpc).withMessage().isEqualTo(chain.rpcUrl)
        }
    }

    @Test
    fun mustReturnDefaultRpcWhenChainDoesNotHaveInfuraRpcDefined() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            val applicationProperties = ApplicationProperties().apply { infuraId = "" }
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct RPC URL is returned") {
            val chain = Chain.HARDHAT_TESTNET
            val rpc = chainPropertiesHandler.getChainRpcUrl(chain)
            assertThat(rpc).withMessage().isEqualTo(chain.rpcUrl)
        }
    }

    @Test
    fun mustReturnInfuraRpc() {
        val infuraId = "some-id"

        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            val applicationProperties = ApplicationProperties().apply { this.infuraId = infuraId }
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct Infura RPC URL is returned") {
            val chain = Chain.MATIC_TESTNET_MUMBAI
            val rpc = chainPropertiesHandler.getChainRpcUrl(chain)
            assertThat(rpc).withMessage().isEqualTo(chain.infura + infuraId)
        }
    }

    private fun ChainId.toSpec() = ChainSpec(this, null)
}
