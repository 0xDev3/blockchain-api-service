package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionHashRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20LockRequest
import com.ampnet.blockchainapiservice.model.response.Erc20LockRequestResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.service.Erc20LockRequestService
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.DurationSeconds
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class Erc20LockRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateErc20LockRequest() {
        val params = CreateErc20LockRequestParams(
            clientId = "client-id",
            chainId = ChainId(123L),
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = DurationSeconds(BigInteger.valueOf(123L)),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = Erc20LockRequest(
            id = UUID.randomUUID(),
            chainId = params.chainId!!,
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress!!,
            tokenAmount = params.tokenAmount,
            lockDuration = params.lockDuration,
            lockContractAddress = params.lockContractAddress,
            tokenSenderAddress = params.tokenSenderAddress,
            txHash = null,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig
        )
        val data = FunctionData("data")
        val service = mock<Erc20LockRequestService>()

        suppose("ERC20 lock request will be created") {
            given(service.createErc20LockRequest(params))
                .willReturn(WithFunctionData(result, data))
        }

        val controller = Erc20LockRequestController(service)

        verify("controller returns correct response") {
            val response = controller.createErc20LockRequest(
                CreateErc20LockRequest(
                    clientId = params.clientId,
                    chainId = params.chainId?.value,
                    redirectUrl = params.redirectUrl,
                    tokenAddress = params.tokenAddress?.rawValue,
                    amount = params.tokenAmount.rawValue,
                    lockDurationInSeconds = params.lockDuration.rawValue,
                    lockContractAddress = params.lockContractAddress.rawValue,
                    senderAddress = params.tokenSenderAddress?.rawValue,
                    arbitraryData = params.arbitraryData,
                    screenConfig = params.screenConfig
                )
            )

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20LockRequestResponse(
                            id = result.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress.rawValue,
                            amount = result.tokenAmount.rawValue,
                            lockDurationInSeconds = params.lockDuration.rawValue,
                            unlocksAt = null,
                            lockContractAddress = result.lockContractAddress.rawValue,
                            senderAddress = result.tokenSenderAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig,
                            redirectUrl = result.redirectUrl,
                            lockTx = TransactionResponse(
                                txHash = null,
                                from = result.tokenSenderAddress?.rawValue,
                                to = result.tokenAddress.rawValue,
                                data = data.value,
                                blockConfirmations = null,
                                timestamp = null
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequest() {
        val id = UUID.randomUUID()
        val rpcSpec = RpcUrlSpec("url", "url-override")
        val service = mock<Erc20LockRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20LockRequest(
                id = id,
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                tokenAmount = Balance(BigInteger.TEN),
                lockDuration = DurationSeconds(BigInteger.valueOf(123L)),
                lockContractAddress = ContractAddress("b"),
                tokenSenderAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some ERC20 lock request will be fetched") {
            given(service.getErc20LockRequest(id, rpcSpec))
                .willReturn(result)
        }

        val controller = Erc20LockRequestController(service)

        verify("controller returns correct response") {
            assertThat(controller.getErc20LockRequest(id, rpcSpec)).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20LockRequestResponse(
                            id = result.value.id,
                            status = result.status,
                            chainId = result.value.chainId.value,
                            tokenAddress = result.value.tokenAddress.rawValue,
                            amount = result.value.tokenAmount.rawValue,
                            lockDurationInSeconds = result.value.lockDuration.rawValue,
                            unlocksAt = (TestData.TIMESTAMP + result.value.lockDuration).value,
                            lockContractAddress = result.value.lockContractAddress.rawValue,
                            senderAddress = result.value.tokenSenderAddress?.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig,
                            redirectUrl = result.value.redirectUrl,
                            lockTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data.value,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = TestData.TIMESTAMP.value
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionHash() {
        val service = mock<Erc20LockRequestService>()
        val controller = Erc20LockRequestController(service)

        val id = UUID.randomUUID()
        val txHash = "tx-hash"

        suppose("transaction hash will be attached") {
            controller.attachTransactionHash(id, AttachTransactionHashRequest(txHash))
        }

        verify("transaction hash is correctly attached") {
            verifyMock(service)
                .attachTxHash(id, TransactionHash(txHash))

            verifyNoMoreInteractions(service)
        }
    }
}
