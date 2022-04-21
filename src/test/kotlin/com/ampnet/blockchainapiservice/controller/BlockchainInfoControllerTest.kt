package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.response.FetchErc20TokenBalanceResponse
import com.ampnet.blockchainapiservice.service.BlockchainInfoService
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class BlockchainInfoControllerTest : TestBase() {

    @Test
    fun mustReturnCorrectErc20TokenBalanceWhenBlockNumberIsNotProvided() {
        val accountBalance = AccountBalance(WalletAddress("123"), Balance(BigInteger("10000")))
        val messageId = UUID.randomUUID()
        val chainSpec = Chain.HARDHAT_TESTNET.id.toSpec()
        val contractAddress = ContractAddress("abc")

        val service = mock<BlockchainInfoService>()

        suppose("service will return some account balance") {
            given(
                service.fetchErc20AccountBalanceFromSignedMessage(
                    messageId = messageId,
                    chainSpec = chainSpec,
                    contractAddress = contractAddress,
                    block = BlockName.LATEST
                )
            )
                .willReturn(accountBalance)
        }

        val controller = BlockchainInfoController(service)

        verify("controller returns correct response") {
            val result = controller.fetchErc20TokenBalance(
                chainSpec = chainSpec,
                messageId = messageId,
                contractAddress = contractAddress,
                blockNumber = null
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        FetchErc20TokenBalanceResponse(
                            walletAddress = accountBalance.address.rawValue,
                            tokenBalance = accountBalance.balance.rawValue,
                            tokenAddress = contractAddress.rawValue
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturnCorrectErc20TokenBalanceForSpecifiedBlockNumber() {
        val accountBalance = AccountBalance(WalletAddress("123"), Balance(BigInteger("10000")))
        val messageId = UUID.randomUUID()
        val chainSpec = Chain.HARDHAT_TESTNET.id.toSpec()
        val contractAddress = ContractAddress("abc")
        val blockNumber = BlockNumber(BigInteger("456"))

        val service = mock<BlockchainInfoService>()

        suppose("service will return some account balance") {
            given(
                service.fetchErc20AccountBalanceFromSignedMessage(
                    messageId = messageId,
                    chainSpec = chainSpec,
                    contractAddress = contractAddress,
                    block = blockNumber
                )
            )
                .willReturn(accountBalance)
        }

        val controller = BlockchainInfoController(service)

        verify("controller returns correct response") {
            val result = controller.fetchErc20TokenBalance(
                chainSpec = chainSpec,
                messageId = messageId,
                contractAddress = contractAddress,
                blockNumber = blockNumber
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        FetchErc20TokenBalanceResponse(
                            walletAddress = accountBalance.address.rawValue,
                            tokenBalance = accountBalance.balance.rawValue,
                            tokenAddress = contractAddress.rawValue
                        )
                    )
                )
        }
    }

    private fun ChainId.toSpec() = ChainSpec(this, RpcUrlSpec(null, null))
}
