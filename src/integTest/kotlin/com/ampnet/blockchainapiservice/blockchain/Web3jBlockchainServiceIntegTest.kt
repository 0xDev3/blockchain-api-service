package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BlockchainReadException
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.service.EthereumFunctionEncoderService
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.Erc20Balance
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Web3jBlockchainServiceIntegTest : TestBase() {

    private val hardhatContainer = HardhatTestContainer()
    private val accounts = HardhatTestContainer.accounts

    @BeforeEach
    fun beforeEach() {
        hardhatContainer.reset()
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceForLatestBlock() {
        val mainAccount = accounts[0]
        val accountBalance = Erc20Balance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        verify("correct ERC20 balance is fetched for latest block") {
            val service = Web3jBlockchainService(hardhatProperties())
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.wallet
            )

            assertThat(fetchedAccountBalance).withMessage()
                .isEqualTo(
                    accountBalance.copy(
                        blockNumber = fetchedAccountBalance.blockNumber,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            assertThat(fetchedAccountBalance.blockNumber.value)
                .isPositive()
            assertThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceForSpecifiedBlockNumber() {
        val mainAccount = accounts[0]
        val accountBalance = Erc20Balance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        val blockNumberBeforeSendingBalance = hardhatContainer.blockNumber()

        suppose("some ERC20 transaction is made") {
            contract.transferAndMine(accounts[1].address, accountBalance.amount.rawValue)
        }

        val service = Web3jBlockchainService(hardhatProperties())

        verify("correct ERC20 balance is fetched for block number before ERC20 transfer is made") {
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.wallet,
                blockParameter = blockNumberBeforeSendingBalance
            )

            assertThat(fetchedAccountBalance).withMessage()
                .isEqualTo(
                    accountBalance.copy(
                        blockNumber = blockNumberBeforeSendingBalance,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            assertThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        val blockNumberAfterSendingBalance = hardhatContainer.blockNumber()

        verify("correct ERC20 balance is fetched for block number after ERC20 transfer is made") {
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.wallet,
                blockParameter = blockNumberAfterSendingBalance
            )

            assertThat(fetchedAccountBalance).withMessage()
                .isEqualTo(
                    accountBalance.copy(
                        amount = Balance(BigInteger.ZERO),
                        blockNumber = blockNumberAfterSendingBalance,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            assertThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
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
    fun mustCorrectlyFetchContractTransactionInfoForSuccessfulTransaction() {
        val mainAccount = accounts[0]
        val accountBalance = Erc20Balance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        val txHash = suppose("some ERC20 transfer transaction is made") {
            contract.transferAndMine(accounts[1].address, accountBalance.amount.rawValue)
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
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = true
                    )
                )

            val expectedData = EthereumFunctionEncoderService().encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(abiType = AbiType.Address, value = WalletAddress(accounts[1].address)),
                    FunctionArgument(abiType = AbiType.Uint256, value = accountBalance.amount)
                ),
                abiOutputTypes = listOf(AbiType.Bool),
                additionalData = listOf()
            )

            assertThat(transactionInfo.data).withMessage()
                .isEqualTo(expectedData)

            assertThat(transactionInfo.blockConfirmations).withMessage()
                .isNotZero()
        }
    }

    @Test
    fun mustCorrectlyFetchContractTransactionInfoForFailedTransaction() {
        val mainAccount = accounts[0]
        val accountBalance = Erc20Balance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )
        val sendAmount = Balance(accountBalance.amount.rawValue * BigInteger.TEN)

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        val txHash = suppose("some ERC20 transfer transaction is made") {
            try {
                contract.transferAndMine(accounts[1].address, sendAmount.rawValue)
                    ?.get()?.transactionHash?.let { TransactionHash(it) }!!
            } catch (e: ExecutionException) {
                (e.cause as? TransactionException)?.transactionReceipt
                    ?.get()?.transactionHash?.let { TransactionHash(it) }!!
            }
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
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = false
                    )
                )

            val expectedData = EthereumFunctionEncoderService().encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(abiType = AbiType.Address, value = WalletAddress(accounts[1].address)),
                    FunctionArgument(abiType = AbiType.Uint256, value = sendAmount)
                ),
                abiOutputTypes = listOf(AbiType.Bool),
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
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = true
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

    private fun ChainId.toSpec() = ChainSpec(this, RpcUrlSpec(null, null))
}
