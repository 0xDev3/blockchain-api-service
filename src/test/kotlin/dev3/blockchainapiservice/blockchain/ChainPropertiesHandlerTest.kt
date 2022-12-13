package dev3.blockchainapiservice.blockchain

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.blockchain.properties.ChainPropertiesHandler
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.config.ApplicationProperties
import dev3.blockchainapiservice.config.ChainProperties
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.exception.UnsupportedChainIdException
import dev3.blockchainapiservice.util.ChainId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChainPropertiesHandlerTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1L)
        private val CHAINS = mapOf(
            CHAIN_ID to ChainProperties(
                name = "ETHEREUM_MAIN",
                rpcUrl = "rpc-url",
                infuraUrl = "/infura/",
                startBlockNumber = null,
                minBlockConfirmationsForCaching = null
            )
        )
    }

    @Test
    fun mustCorrectlyCreateChainPropertiesWithServicesWhenRpcUrlIsNull() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(
                ApplicationProperties()
                    .apply {
                        infuraId = ""
                        chain = CHAINS
                    }
            )
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(CHAIN_ID.toSpec())
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
            ChainPropertiesHandler(
                ApplicationProperties()
                    .apply {
                        infuraId = ""
                        chain = CHAINS
                    }
            )
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(
                ChainSpec(
                    chainId = CHAIN_ID,
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
    fun mustReturnDefaultRpcIfInfuraIdIsMissing() {
        val applicationProperties = ApplicationProperties()
            .apply {
                infuraId = ""
                chain = CHAINS
            }

        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct RPC URL is returned") {
            val chainProperties = applicationProperties.chain[CHAIN_ID]!!
            val rpc = chainPropertiesHandler.getChainRpcUrl(chainProperties)
            assertThat(rpc).withMessage().isEqualTo(chainProperties.rpcUrl)
        }
    }

    @Test
    fun mustReturnDefaultRpcWhenChainDoesNotHaveInfuraRpcDefined() {
        val applicationProperties = ApplicationProperties()
            .apply {
                infuraId = ""
                chain = CHAINS
            }

        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct RPC URL is returned") {
            val chainProperties = applicationProperties.chain[CHAIN_ID]!!
            val rpc = chainPropertiesHandler.getChainRpcUrl(chainProperties)
            assertThat(rpc).withMessage().isEqualTo(chainProperties.rpcUrl)
        }
    }

    @Test
    fun mustReturnInfuraRpc() {
        val infuraId = "some-id"
        val applicationProperties = ApplicationProperties()
            .apply {
                this.infuraId = infuraId
                chain = CHAINS
            }

        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct Infura RPC URL is returned") {
            val chainProperties = applicationProperties.chain[CHAIN_ID]!!
            val rpc = chainPropertiesHandler.getChainRpcUrl(chainProperties)
            assertThat(rpc).withMessage().isEqualTo(chainProperties.infuraUrl + infuraId)
        }
    }

    private fun ChainId.toSpec() = ChainSpec(this, null)
}
