package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BlockchainReadException
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.service.EthereumFunctionEncoderService
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.CompletableFuture

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

    @Test
    fun mustCorrectlyFetchContractTransactionInfo() {
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

        val txHash = suppose("some ERC20 transfer transaction is made") {
            contract.transferAndMine(accounts[1].address, accountBalance.balance.rawValue)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        verify("correct transaction info is fetched") {
            val service = Web3jBlockchainService(hardhatProperties())
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                txHash = txHash
            )

            assertThat(transactionInfo).withMessage()
                .isNotNull()

            assertThat(transactionInfo!!).withMessage()
                .isEqualTo(
                    BlockchainTransactionInfo(
                        hash = txHash,
                        from = WalletAddress(mainAccount.address),
                        to = WalletAddress(contract.contractAddress),
                        data = transactionInfo.data,
                        blockConfirmations = transactionInfo.blockConfirmations
                    )
                )

            val expectedData = EthereumFunctionEncoderService().encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(abiType = "address", value = accounts[1].address),
                    FunctionArgument(abiType = "uint256", value = accountBalance.balance.rawValue)
                ),
                abiOutputTypes = listOf("bool"),
                additionalData = listOf()
            )

            assertThat(transactionInfo.data).withMessage()
                .isEqualTo(expectedData)

            assertThat(transactionInfo.blockConfirmations).withMessage()
                .isNotZero()
        }
    }

    @Test
    fun mustCorrectlyFetchNonContractTransactionInfo() {
        val mainAccount = accounts[0]
        val txHash = suppose("some ERC20 transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accounts[1].address,
                BigDecimal.ONE,
                Convert.Unit.ETHER
            ).sendAsync()?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        verify("correct transaction info is fetched") {
            val service = Web3jBlockchainService(hardhatProperties())
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                txHash = txHash
            )

            assertThat(transactionInfo).withMessage()
                .isNotNull()

            assertThat(transactionInfo!!).withMessage()
                .isEqualTo(
                    BlockchainTransactionInfo(
                        hash = txHash,
                        from = WalletAddress(mainAccount.address),
                        to = WalletAddress(accounts[1].address),
                        data = FunctionData("0x"),
                        blockConfirmations = transactionInfo.blockConfirmations
                    )
                )

            assertThat(transactionInfo.blockConfirmations).withMessage()
                .isNotZero()
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentTransactionInfo() {
        verify("null is returned for non existent transaction") {
            val service = Web3jBlockchainService(hardhatProperties())
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                txHash = TransactionHash("0x123456")
            )

            assertThat(transactionInfo).withMessage()
                .isNull()
        }
    }

    private fun <T> RemoteCall<T>.sendAndMine(): T {
        val future = sendAsync()
        hardhatContainer.waitAndMine()
        return future.get()
    }

    private fun SimpleERC20.transferAndMine(
        address: String,
        amount: BigInteger
    ): CompletableFuture<TransactionReceipt?>? {
        val txReceiptFuture = transfer(address, amount).sendAsync()
        hardhatContainer.mineUntil {
            balanceOf(address).send() == amount
        }
        return txReceiptFuture
    }

    private fun hardhatProperties() = ApplicationProperties().apply { infuraId = hardhatContainer.mappedPort }

    private fun ChainId.toSpec() = ChainSpec(this, null)
}
