package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BlockchainReadException
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.web3j.protocol.core.RemoteCall
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Web3jBlockchainServiceIntegTest : TestBase() {

    private val hardhatContainer = HardhatTestContainer()
    private val accounts = HardhatTestContainer.accounts

    @Test
    fun mustCorrectlyFetchErc20BalanceForLatestBlock() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(WalletAddress(mainAccount.address), Balance(BigInteger("10000")))

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.address.rawValue),
                listOf(accountBalance.balance.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        verify("correct ERC20 balance is fetched for latest block") {
            val service = Web3jBlockchainService(hardhatProperties())
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.address
            )

            assertThat(fetchedAccountBalance).withMessage()
                .isEqualTo(accountBalance)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceForSpecifiedBlockNumber() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(WalletAddress(mainAccount.address), Balance(BigInteger("10000")))

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.address.rawValue),
                listOf(accountBalance.balance.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        val blockNumberBeforeSendingBalance = hardhatContainer.blockNumber()

        suppose("some ERC20 transaction is made") {
            contract.transferAndMine(accounts[1].address, accountBalance.balance.rawValue)
        }

        val service = Web3jBlockchainService(hardhatProperties())

        verify("correct ERC20 balance is fetched for block number before ERC20 transfer is made") {
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.address,
                block = blockNumberBeforeSendingBalance
            )

            assertThat(fetchedAccountBalance).withMessage()
                .isEqualTo(accountBalance)
        }

        val blockNumberAfterSendingBalance = hardhatContainer.blockNumber()

        verify("correct ERC20 balance is fetched for block number after ERC20 transfer is made") {
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.address,
                block = blockNumberAfterSendingBalance
            )

            assertThat(fetchedAccountBalance).withMessage()
                .isEqualTo(accountBalance.copy(balance = Balance(BigInteger.ZERO)))
        }
    }

    @Test
    fun mustThrowBlockchainReadExceptionWhenReadingErc20BalanceFromInvalidErc20ContractAddress() {
        verify("BlockchainReadException is thrown when reading ERC20 balance from invalid contract address") {
            val service = Web3jBlockchainService(hardhatProperties())

            assertThrows<BlockchainReadException>(message) {
                service.fetchErc20AccountBalance(
                    chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                    contractAddress = ContractAddress(accounts[0].address),
                    walletAddress = WalletAddress(accounts[0].address)
                )
            }
        }
    }

    private fun <T> RemoteCall<T>.sendAndMine(): T {
        val future = sendAsync()
        hardhatContainer.waitAndMine()
        return future.get()
    }

    private fun SimpleERC20.transferAndMine(address: String, amount: BigInteger) {
        transfer(address, amount).sendAsync()
        hardhatContainer.mineUntil {
            balanceOf(address).send() == amount
        }
    }

    private fun hardhatProperties() = ApplicationProperties().apply { infuraId = hardhatContainer.mappedPort }

    private fun ChainId.toSpec() = ChainSpec(this, null)
}
