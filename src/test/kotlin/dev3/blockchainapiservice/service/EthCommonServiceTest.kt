package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.params.ParamsFactory
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.math.BigInteger
import java.util.UUID

class EthCommonServiceTest : TestBase() {

    companion object {
        data class InParams(val value: Int)
        data class OutParams(val id: UUID, val params: InParams, val project: Project, val createdAt: UtcDateTime)
        object Factory : ParamsFactory<InParams, OutParams> {
            override fun fromCreateParams(id: UUID, params: InParams, project: Project, createdAt: UtcDateTime) =
                OutParams(id, params, project, createdAt)
        }
    }

    @Test
    fun mustCorrectlyCreateDatabaseParams() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getRawUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val params = InParams(1)
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = EthCommonServiceImpl(
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            blockchainService = mock()
        )

        verify("correct result is returned") {
            val result = service.createDatabaseParams(Factory, params, project)

            expectThat(result)
                .isEqualTo(OutParams(uuid, params, project, TestData.TIMESTAMP))
        }
    }

    @Test
    fun mustCorrectlyFetchNonNullResource() {
        val service = EthCommonServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            blockchainService = mock()
        )
        val input = "test"

        verify("correct result is returned") {
            val output = service.fetchResource(input, "message")

            expectThat(output)
                .isEqualTo(input)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNullResource() {
        val service = EthCommonServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            blockchainService = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.fetchResource(null, "message")
            }
        }
    }

    @Test
    fun mustCorrectlyFetchTransactionInfoWhenTxHashIsNotNull() {
        val chainSpec = ChainSpec(
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url"
        )
        val txHash = TransactionHash("tx-hash")
        val transactionInfo = BlockchainTransactionInfo(
            hash = txHash,
            from = WalletAddress("a"),
            to = WalletAddress("b"),
            deployedContractAddress = null,
            data = FunctionData("data"),
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ZERO,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        val blockchainService = mock<BlockchainService>()

        suppose("some transaction info is fetched from blockchain") {
            call(blockchainService.fetchTransactionInfo(chainSpec, txHash, emptyList()))
                .willReturn(transactionInfo)
        }

        val service = EthCommonServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            blockchainService = blockchainService
        )

        verify("correct result is returned") {
            val result = service.fetchTransactionInfo(
                txHash = txHash,
                chainId = chainSpec.chainId,
                customRpcUrl = chainSpec.customRpcUrl,
                events = emptyList()
            )

            expectThat(result)
                .isEqualTo(transactionInfo)
        }
    }

    @Test
    fun mustReturnNullWhenTxHashIsNull() {
        val service = EthCommonServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            blockchainService = mock()
        )

        verify("null is returned") {
            val result = service.fetchTransactionInfo(
                txHash = null,
                chainId = TestData.CHAIN_ID,
                customRpcUrl = null,
                events = emptyList()
            )

            expectThat(result)
                .isNull()
        }
    }
}
