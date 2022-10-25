package dev3.blockchainapiservice.blockchain

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.blockchain.properties.Chain
import dev3.blockchainapiservice.blockchain.properties.ChainPropertiesHandler
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.config.ApplicationProperties
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.exception.UnsupportedChainIdException
import dev3.blockchainapiservice.util.ChainId
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
    fun mustCorrectlyCreateChainPropertiesWithServicesWhenCustomRpcUrlIsSpecified() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(ApplicationProperties().apply { infuraId = "" })
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(
                ChainSpec(
                    chainId = ChainId(123L),
                    customRpcUrl = "http://localhost:1234/"
                )
            )
            assertThat(chainProperties.web3j).withMessage().isNotNull()
        }
    }

    @Test
    fun mustCorrectlyCreateChainPropertiesWithServicesWhenCustomRpcUrlIsNotSpecified() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(ApplicationProperties().apply { infuraId = "" })
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(
                ChainSpec(
                    chainId = Chain.HARDHAT_TESTNET.id,
                    customRpcUrl = null
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
    fun mustReturnOverriddenRpcUrl() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            val applicationProperties = ApplicationProperties()
                .apply {
                    chainMumbai.rpcUrlOverride = "override"
                    infuraId = ""
                }
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct RPC URL is returned") {
            val chain = Chain.MATIC_TESTNET_MUMBAI
            val rpc = chainPropertiesHandler.getChainRpcUrl(chain)
            assertThat(rpc).withMessage().isEqualTo("override")
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
