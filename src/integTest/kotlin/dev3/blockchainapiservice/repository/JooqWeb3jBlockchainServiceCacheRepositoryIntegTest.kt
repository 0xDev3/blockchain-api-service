package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentTransactionCacheId
import dev3.blockchainapiservice.generated.jooq.id.FetchAccountBalanceCacheId
import dev3.blockchainapiservice.generated.jooq.id.FetchErc20AccountBalanceCacheId
import dev3.blockchainapiservice.generated.jooq.id.FetchTransactionInfoCacheId
import dev3.blockchainapiservice.model.EventLog
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.FullContractDeploymentTransactionInfo
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import dev3.blockchainapiservice.util.AccountBalance
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.ZeroAddress
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqWeb3jBlockchainServiceCacheRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqWeb3jBlockchainServiceCacheRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_SPEC = ChainSpec(
            chainId = ChainId(123L),
            customRpcUrl = null
        )
        private val BLOCK_NUMBER = BlockNumber(BigInteger.valueOf(456L))
        private val ACCOUNT_BALANCE = AccountBalance(
            wallet = WalletAddress("abc"),
            blockNumber = BLOCK_NUMBER,
            timestamp = TestData.TIMESTAMP,
            amount = Balance(BigInteger.valueOf(1000L))
        )
        private val CONTRACT_ADDRESS = ContractAddress("def")
        private val TX_INFO = BlockchainTransactionInfo(
            hash = TransactionHash("tx-hash"),
            from = WalletAddress("123"),
            to = ZeroAddress.toWalletAddress(),
            deployedContractAddress = CONTRACT_ADDRESS,
            data = FunctionData("1234"),
            value = Balance(BigInteger.TEN),
            blockConfirmations = BigInteger.TEN,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )
        val CONTRACT_DEPLOYMENT_TRANSACTION_INFO = FullContractDeploymentTransactionInfo(
            hash = TX_INFO.hash,
            from = TX_INFO.from,
            deployedContractAddress = CONTRACT_ADDRESS,
            data = TX_INFO.data,
            value = TX_INFO.value,
            binary = ContractBinaryData("abc1223"),
            blockNumber = BLOCK_NUMBER,
            events = emptyList()
        )
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqWeb3jBlockchainServiceCacheRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyCacheFetchAccountBalance() {
        val id = FetchAccountBalanceCacheId(UUID.randomUUID())

        suppose("fetchAccountBalance call will be cached") {
            repository.cacheFetchAccountBalance(
                id = id,
                chainSpec = CHAIN_SPEC,
                accountBalance = ACCOUNT_BALANCE
            )
        }

        verify("fetchAccountBalance call is correctly cached") {
            expectThat(
                repository.getCachedFetchAccountBalance(
                    chainSpec = CHAIN_SPEC,
                    walletAddress = ACCOUNT_BALANCE.wallet,
                    blockNumber = ACCOUNT_BALANCE.blockNumber
                )
            ).isEqualTo(ACCOUNT_BALANCE)
        }
    }

    @Test
    fun mustCorrectlyCacheFetchErc20AccountBalance() {
        val id = FetchErc20AccountBalanceCacheId(UUID.randomUUID())

        suppose("fetchErc20AccountBalance call will be cached") {
            repository.cacheFetchErc20AccountBalance(
                id = id,
                chainSpec = CHAIN_SPEC,
                contractAddress = CONTRACT_ADDRESS,
                accountBalance = ACCOUNT_BALANCE
            )
        }

        verify("fetchErc20AccountBalance call is correctly cached") {
            expectThat(
                repository.getCachedFetchErc20AccountBalance(
                    chainSpec = CHAIN_SPEC,
                    contractAddress = CONTRACT_ADDRESS,
                    walletAddress = ACCOUNT_BALANCE.wallet,
                    blockNumber = ACCOUNT_BALANCE.blockNumber
                )
            ).isEqualTo(ACCOUNT_BALANCE)
        }
    }

    @Test
    fun mustCorrectlyCacheFetchTransactionInfo() {
        val id = FetchTransactionInfoCacheId(UUID.randomUUID())

        suppose("fetchTransactionInfo call will be cached") {
            repository.cacheFetchTransactionInfo(
                id = id,
                chainSpec = CHAIN_SPEC,
                txHash = TX_INFO.hash,
                blockNumber = BlockNumber(BLOCK_NUMBER.value - TX_INFO.blockConfirmations),
                txInfo = TX_INFO,
                eventLogs = listOf(
                    EventLog(
                        data = "data",
                        topics = listOf("topic")
                    )
                )
            )
        }

        verify("fetchTransactionInfo call is correctly cached") {
            expectThat(
                repository.getCachedFetchTransactionInfo(
                    chainSpec = CHAIN_SPEC,
                    txHash = TX_INFO.hash,
                    currentBlockNumber = BLOCK_NUMBER
                )
            ).isEqualTo(Pair(TX_INFO, listOf(EventLog("data", listOf("topic")))))
        }
    }

    @Test
    fun mustCorrectlyCacheContractDeploymentTransaction() {
        val id = ContractDeploymentTransactionCacheId(UUID.randomUUID())

        suppose("findContractDeploymentTransaction call will be cached") {
            repository.cacheContractDeploymentTransaction(
                id = id,
                chainSpec = CHAIN_SPEC,
                contractAddress = CONTRACT_ADDRESS,
                contractDeploymentTransactionInfo = CONTRACT_DEPLOYMENT_TRANSACTION_INFO,
                eventLogs = listOf(
                    EventLog(
                        data = "data",
                        topics = listOf("topic")
                    )
                )
            )
        }

        verify("findContractDeploymentTransaction call is correctly cached") {
            expectThat(
                repository.getCachedContractDeploymentTransaction(
                    chainSpec = CHAIN_SPEC,
                    contractAddress = CONTRACT_ADDRESS
                )
            ).isEqualTo(Pair(CONTRACT_DEPLOYMENT_TRANSACTION_INFO, listOf(EventLog("data", listOf("topic")))))
        }
    }
}
