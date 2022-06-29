package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.ParamsFactory
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.math.BigInteger
import java.util.UUID

class Erc20CommonServiceTest : TestBase() {

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
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val params = InParams(1)
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = Erc20CommonServiceImpl(
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            blockchainService = mock()
        )

        verify("correct result is returned") {
            val result = service.createDatabaseParams(Factory, params, project)

            assertThat(result).withMessage()
                .isEqualTo(OutParams(uuid, params, project, TestData.TIMESTAMP))
        }
    }

    @Test
    fun mustCorrectlyFetchNonNullResource() {
        val service = Erc20CommonServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            blockchainService = mock()
        )
        val input = "test"

        verify("correct result is returned") {
            val output = service.fetchResource(input, "message")

            assertThat(output).withMessage()
                .isEqualTo(input)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNullResource() {
        val service = Erc20CommonServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            blockchainService = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.fetchResource(null, "message")
            }
        }
    }

    @Test
    fun mustCorrectlyFetchTransactionInfoWhenTxHashIsNotNull() {
        val chainSpec = ChainSpec(
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url"
        )
        val txHash = TransactionHash("tx-hash")
        val transactionInfo = BlockchainTransactionInfo(
            hash = txHash,
            from = WalletAddress("a"),
            to = WalletAddress("b"),
            data = FunctionData("data"),
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ZERO,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        val blockchainService = mock<BlockchainService>()

        suppose("some transaction info is fetched from blockchain") {
            given(blockchainService.fetchTransactionInfo(chainSpec, txHash))
                .willReturn(transactionInfo)
        }

        val service = Erc20CommonServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            blockchainService = blockchainService
        )

        verify("correct result is returned") {
            val result = service.fetchTransactionInfo(
                txHash = txHash,
                chainId = chainSpec.chainId,
                customRpcUrl = chainSpec.customRpcUrl
            )

            assertThat(result).withMessage()
                .isEqualTo(transactionInfo)
        }
    }

    @Test
    fun mustReturnNullWhenTxHashIsNull() {
        val service = Erc20CommonServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            blockchainService = mock()
        )

        verify("null is returned") {
            val result = service.fetchTransactionInfo(
                txHash = null,
                chainId = Chain.HARDHAT_TESTNET.id,
                customRpcUrl = null
            )

            assertThat(result).withMessage()
                .isNull()
        }
    }
}
