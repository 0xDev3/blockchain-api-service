package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BlockchainReadException
import com.ampnet.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.params.OutputParameter
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.service.CurrentUtcDateTimeProvider
import com.ampnet.blockchainapiservice.service.EthereumAbiDecoderService
import com.ampnet.blockchainapiservice.service.EthereumFunctionEncoderService
import com.ampnet.blockchainapiservice.service.RandomUuidProvider
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UintType
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.ZeroAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Uint
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Web3jBlockchainServiceIntegTest : TestBase() {

    private val hardhatContainer = SharedTestContainers.hardhatContainer
    private val accounts = HardhatTestContainer.accounts

    @BeforeEach
    fun beforeEach() {
        hardhatContainer.reset()
    }

    @Test
    fun mustCorrectlyFetchAccountBalanceForLatestBlock() {
        val mainAccount = accounts[0]
        val value = Convert.toWei(BigDecimal.ONE, Convert.Unit.ETHER)
        val accountBalance = AccountBalance(
            wallet = WalletAddress("cafebabe"),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(value.toBigInteger())
        )

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accountBalance.wallet.rawValue,
                value,
                Convert.Unit.WEI
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("correct account balance is fetched for latest block") {
            val service = createService()
            service.fetchAccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                walletAddress = accountBalance.wallet
            )
            val fetchedAccountBalance = service.fetchAccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
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
    fun mustCorrectlyFetchAccountBalanceForSpecifiedBlockNumber() {
        val mainAccount = accounts[0]
        val value = Convert.toWei(BigDecimal.ONE, Convert.Unit.ETHER)
        val accountBalance = AccountBalance(
            wallet = WalletAddress("cafebabe"),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(value.toBigInteger())
        )

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accountBalance.wallet.rawValue,
                value,
                Convert.Unit.WEI
            ).send()
        }

        val blockNumberBeforeSendingBalance = hardhatContainer.blockNumber()

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accountBalance.wallet.rawValue,
                value,
                Convert.Unit.WEI
            ).send()
        }

        val service = createService()

        verify("correct ETH balance is fetched for block number before ETH transfer is made") {
            val fetchedAccountBalance = service.fetchAccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
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

        verify("correct ETH balance is fetched for block number after ETH transfer is made") {
            val fetchedAccountBalance = service.fetchAccountBalance(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                walletAddress = accountBalance.wallet,
                blockParameter = blockNumberAfterSendingBalance
            )

            assertThat(fetchedAccountBalance).withMessage()
                .isEqualTo(
                    accountBalance.copy(
                        amount = Balance(value.toBigInteger().multiply(BigInteger.TWO)),
                        blockNumber = blockNumberAfterSendingBalance,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            assertThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceForLatestBlock() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
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
            ).send()
        }

        verify("correct ERC20 balance is fetched for latest block") {
            val service = createService()
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
        val accountBalance = AccountBalance(
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
            ).send()
        }

        val blockNumberBeforeSendingBalance = hardhatContainer.blockNumber()

        suppose("some ERC20 transaction is made") {
            contract.transfer(accounts[1].address, accountBalance.amount.rawValue).send()
        }

        val service = createService()

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
                        amount = Balance.ZERO,
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
            val service = createService()

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
        val accountBalance = AccountBalance(
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
            ).send()
        }

        val txHash = suppose("some ERC20 transfer transaction is made") {
            contract.transfer(accounts[1].address, accountBalance.amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("correct transaction info is fetched") {
            val service = createService()
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
                        deployedContractAddress = null,
                        data = transactionInfo.data,
                        value = Balance.ZERO,
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = true
                    )
                )

            val expectedData = EthereumFunctionEncoderService().encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(WalletAddress(accounts[1].address)),
                    FunctionArgument(accountBalance.amount)
                )
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
        val accountBalance = AccountBalance(
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
            ).send()
        }

        val txHash = suppose("some ERC20 transfer transaction is made") {
            try {
                contract.transfer(accounts[1].address, sendAmount.rawValue).send()
                    ?.transactionHash?.let { TransactionHash(it) }!!
            } catch (e: TransactionException) {
                // web3j is really something...
                e.message?.removePrefix("{\"txHash\":\"")?.removeSuffix("\"}")
                    ?.let { TransactionHash(it) }!!
            }
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("correct transaction info is fetched") {
            val service = createService()
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
                        deployedContractAddress = null,
                        data = transactionInfo.data,
                        value = Balance.ZERO,
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = false
                    )
                )

            val expectedData = EthereumFunctionEncoderService().encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(WalletAddress(accounts[1].address)),
                    FunctionArgument(sendAmount)
                )
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
        val value = Convert.toWei(BigDecimal.ONE, Convert.Unit.ETHER)
        val txHash = suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accounts[1].address,
                value,
                Convert.Unit.WEI
            ).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("correct transaction info is fetched") {
            val service = createService()
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
                        deployedContractAddress = null,
                        data = FunctionData("0x"),
                        value = Balance(value.toBigInteger()),
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
    fun mustCorrectlyFetchContractDeploymentTransactionInfo() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
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
            ).send()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val encodedConstructor = EthereumFunctionEncoderService().encodeConstructor(
            arguments = listOf(
                FunctionArgument(DynamicArray(Address::class.java, listOf(accountBalance.wallet.value))),
                FunctionArgument(DynamicArray(Uint::class.java, listOf(accountBalance.amount.value))),
                FunctionArgument(WalletAddress(mainAccount.address))
            )
        )
        val data = "0x" + SimpleERC20.BINARY + encodedConstructor.withoutPrefix

        verify("correct transaction info is fetched") {
            val service = createService()
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
                        to = ZeroAddress.toWalletAddress(),
                        deployedContractAddress = ContractAddress(contract.contractAddress),
                        data = FunctionData(data),
                        value = Balance.ZERO,
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
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                txHash = TransactionHash("0x123456")
            )

            assertThat(transactionInfo).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyCallReadonlyFunction() {
        val mainAccount = accounts[0]

        val contract = suppose("readonly function calls contract is deployed") {
            ReadonlyFunctionCallsContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val functionName = "returningUint"
        val uintValue = Uint256(BigInteger.TEN)
        val functionData = EthereumFunctionEncoderService().encode(
            functionName = functionName,
            arguments = listOf(FunctionArgument(uintValue))
        )

        verify("correct value is returned for latest block") {
            val service = createService()
            val result = service.callReadonlyFunction(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                params = ExecuteReadonlyFunctionCallParams(
                    contractAddress = ContractAddress(contract.contractAddress),
                    callerAddress = WalletAddress("a"),
                    functionName = functionName,
                    functionData = functionData,
                    outputParams = listOf(OutputParameter(UintType))
                )
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    ReadonlyFunctionCallResult(
                        blockNumber = result.blockNumber,
                        timestamp = result.timestamp,
                        returnValues = listOf(uintValue.value),
                        rawReturnValue = EthereumFunctionEncoderService()
                            .encodeConstructor(listOf(FunctionArgument(uintValue))).value
                    )
                )
            assertThat(result.blockNumber.value)
                .isPositive()
            assertThat(result.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustThrowBlockchainReadExceptionWhenCallingReadonlyFunctionOnInvalidContractAddress() {
        val functionName = "returningUint"
        val uintValue = Uint256(BigInteger.TEN)
        val functionData = EthereumFunctionEncoderService().encode(
            functionName = functionName,
            arguments = listOf(FunctionArgument(uintValue))
        )

        verify("BlockchainReadException is thrown when calling readonly function on invalid contract address") {
            val service = createService()

            assertThrows<BlockchainReadException>(message) {
                service.callReadonlyFunction(
                    chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                    params = ExecuteReadonlyFunctionCallParams(
                        contractAddress = ContractAddress("dead"),
                        callerAddress = WalletAddress("a"),
                        functionName = functionName,
                        functionData = functionData,
                        outputParams = listOf(OutputParameter(UintType))
                    )
                )
            }
        }
    }

    @Test
    fun mustCorrectlyFindContractDeploymentTransaction() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        suppose("1000 blocks will be mined before contract deployment transaction") {
            repeat(1_000) { hardhatContainer.mine() }
        }

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).send()
        }

        val deploymentTransaction = contract.transactionReceipt.get()

        suppose("1000 blocks will be mined after contract deployment transaction") {
            repeat(1_000) { hardhatContainer.mine() }
        }

        val encodedConstructor = EthereumFunctionEncoderService().encodeConstructor(
            arguments = listOf(
                FunctionArgument(DynamicArray(Address::class.java, listOf(accountBalance.wallet.value))),
                FunctionArgument(DynamicArray(Uint::class.java, listOf(accountBalance.amount.value))),
                FunctionArgument(WalletAddress(mainAccount.address))
            )
        )
        val data = "0x" + SimpleERC20.BINARY + encodedConstructor.withoutPrefix
        val rawBinaryIndex = SimpleERC20.BINARY.lastIndexOf("608060")

        verify("contract deployment transaction is correctly found") {
            val service = createService()
            val transactionInfo = service.findContractDeploymentTransaction(
                chainSpec = Chain.HARDHAT_TESTNET.id.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress)
            )

            assertThat(transactionInfo).withMessage()
                .isNotNull()

            assertThat(transactionInfo!!).withMessage()
                .isEqualTo(
                    ContractDeploymentTransactionInfo(
                        hash = TransactionHash(deploymentTransaction.transactionHash),
                        from = WalletAddress(mainAccount.address),
                        deployedContractAddress = ContractAddress(contract.contractAddress),
                        data = FunctionData(data),
                        value = Balance.ZERO,
                        binary = ContractBinaryData(SimpleERC20.BINARY.substring(rawBinaryIndex))
                    )
                )
        }
    }

    private fun hardhatProperties() = ApplicationProperties().apply { infuraId = hardhatContainer.mappedPort }

    private fun ChainId.toSpec() = ChainSpec(this, null)

    private fun createService() =
        Web3jBlockchainService(
            abiDecoderService = EthereumAbiDecoderService(),
            uuidProvider = RandomUuidProvider(),
            utcDateTimeProvider = CurrentUtcDateTimeProvider(),
            web3jBlockchainServiceCacheRepository = mock(),
            applicationProperties = hardhatProperties()
        )
}
