package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.ClientIdParam
import com.ampnet.blockchainapiservice.model.params.ParamsFactory
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
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
        data class InParams(override val clientId: String?) : ClientIdParam
        data class OutParams(val id: UUID, val params: InParams, val clientInfo: ClientInfo)
        object Factory : ParamsFactory<InParams, OutParams> {
            override fun fromCreateParams(id: UUID, params: InParams, clientInfo: ClientInfo) =
                OutParams(id, params, clientInfo)
        }
    }

    @Test
    fun mustCorrectlyCreateDatabaseParamsWhenClientIdIsSpecified() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val clientId = "test-client-id"
        val clientInfo = ClientInfo(
            clientId = clientId,
            chainId = Chain.HARDHAT_TESTNET.id,
            sendRedirectUrl = "send-redirect-url",
            balanceRedirectUrl = "balance-redirect-url",
            tokenAddress = ContractAddress("a")
        )
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info exists in database") {
            given(clientInfoRepository.getById(clientId))
                .willReturn(clientInfo)
        }

        val params = InParams(clientId)
        val service = Erc20CommonServiceImpl(
            uuidProvider = uuidProvider,
            clientInfoRepository = clientInfoRepository,
            blockchainService = mock()
        )

        verify("correct result is returned") {
            val result = service.createDatabaseParams(Factory, params)

            assertThat(result).withMessage()
                .isEqualTo(OutParams(uuid, params, clientInfo))
        }
    }

    @Test
    fun mustCorrectlyCreateDatabaseParamsWhenClientIdIsNotSpecified() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val params = InParams(null)
        val service = Erc20CommonServiceImpl(
            uuidProvider = uuidProvider,
            clientInfoRepository = mock(),
            blockchainService = mock()
        )

        verify("correct result is returned") {
            val result = service.createDatabaseParams(Factory, params)

            assertThat(result).withMessage()
                .isEqualTo(OutParams(uuid, params, ClientInfo.EMPTY))
        }
    }

    @Test
    fun mustThrowNonExistentClientIdExceptionForNonExistentClientId() {
        val clientId = "test-client-id"
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info does not exist in database") {
            given(clientInfoRepository.getById(clientId))
                .willReturn(null)
        }

        val service = Erc20CommonServiceImpl(
            uuidProvider = mock(),
            clientInfoRepository = clientInfoRepository,
            blockchainService = mock()
        )

        verify("NonExistentClientIdException is thrown") {
            assertThrows<NonExistentClientIdException>(message) {
                service.createDatabaseParams(Factory, InParams(clientId))
            }
        }
    }

    @Test
    fun mustCorrectlyFetchNonNullResource() {
        val service = Erc20CommonServiceImpl(
            uuidProvider = mock(),
            clientInfoRepository = mock(),
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
            clientInfoRepository = mock(),
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
            rpcSpec = RpcUrlSpec("1", "2")
        )
        val txHash = TransactionHash("tx-hash")
        val transactionInfo = BlockchainTransactionInfo(
            hash = txHash,
            from = WalletAddress("a"),
            to = WalletAddress("b"),
            data = FunctionData("data"),
            blockConfirmations = BigInteger.ZERO
        )

        val blockchainService = mock<BlockchainService>()

        suppose("some transaction info is fetched from blockchain") {
            given(blockchainService.fetchTransactionInfo(chainSpec, txHash))
                .willReturn(transactionInfo)
        }

        val service = Erc20CommonServiceImpl(
            uuidProvider = mock(),
            clientInfoRepository = mock(),
            blockchainService = blockchainService
        )

        verify("correct result is returned") {
            val result = service.fetchTransactionInfo(
                txHash = txHash,
                chainId = chainSpec.chainId,
                rpcSpec = chainSpec.rpcSpec
            )

            assertThat(result).withMessage()
                .isEqualTo(transactionInfo)
        }
    }

    @Test
    fun mustReturnNullWhenTxHashIsNull() {
        val service = Erc20CommonServiceImpl(
            uuidProvider = mock(),
            clientInfoRepository = mock(),
            blockchainService = mock()
        )

        verify("null is returned") {
            val result = service.fetchTransactionInfo(
                txHash = null,
                chainId = Chain.HARDHAT_TESTNET.id,
                rpcSpec = RpcUrlSpec(null, null)
            )

            assertThat(result).withMessage()
                .isNull()
        }
    }
}
