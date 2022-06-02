package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionHashRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20SendRequest
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.service.Erc20SendRequestService
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
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

class Erc20SendRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateErc20SendRequest() {
        val params = CreateErc20SendRequestParams(
            clientId = "client-id",
            chainId = ChainId(123L),
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = Erc20SendRequest(
            id = UUID.randomUUID(),
            chainId = params.chainId!!,
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress!!,
            tokenAmount = params.tokenAmount,
            tokenSenderAddress = params.tokenSenderAddress,
            tokenRecipientAddress = params.tokenRecipientAddress,
            txHash = null,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig
        )
        val data = FunctionData("data")
        val service = mock<Erc20SendRequestService>()

        suppose("ERC20 send request will be created") {
            given(service.createErc20SendRequest(params))
                .willReturn(WithFunctionData(result, data))
        }

        val controller = Erc20SendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.createErc20SendRequest(
                CreateErc20SendRequest(
                    clientId = params.clientId,
                    chainId = params.chainId?.value,
                    redirectUrl = params.redirectUrl,
                    tokenAddress = params.tokenAddress?.rawValue,
                    amount = params.tokenAmount.rawValue,
                    senderAddress = params.tokenSenderAddress?.rawValue,
                    recipientAddress = params.tokenRecipientAddress.rawValue,
                    arbitraryData = params.arbitraryData,
                    screenConfig = params.screenConfig
                )
            )

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20SendRequestResponse(
                            id = result.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress.rawValue,
                            amount = result.tokenAmount.rawValue,
                            senderAddress = result.tokenSenderAddress?.rawValue,
                            recipientAddress = result.tokenRecipientAddress.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig,
                            redirectUrl = result.redirectUrl,
                            sendTx = TransactionResponse(
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
    fun mustCorrectlyFetchErc20SendRequest() {
        val id = UUID.randomUUID()
        val rpcSpec = RpcUrlSpec("url", "url-override")
        val service = mock<Erc20SendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20SendRequest(
                id = id,
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                tokenAmount = Balance(BigInteger.TEN),
                tokenSenderAddress = WalletAddress("b"),
                tokenRecipientAddress = WalletAddress("c"),
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

        suppose("some ERC20 send request will be fetched") {
            given(service.getErc20SendRequest(id, rpcSpec))
                .willReturn(result)
        }

        val controller = Erc20SendRequestController(service)

        verify("controller returns correct response") {
            assertThat(controller.getErc20SendRequest(id, rpcSpec)).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20SendRequestResponse(
                            id = result.value.id,
                            status = result.status,
                            chainId = result.value.chainId.value,
                            tokenAddress = result.value.tokenAddress.rawValue,
                            amount = result.value.tokenAmount.rawValue,
                            senderAddress = result.value.tokenSenderAddress?.rawValue,
                            recipientAddress = result.value.tokenRecipientAddress.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig,
                            redirectUrl = result.value.redirectUrl,
                            sendTx = TransactionResponse(
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
        val service = mock<Erc20SendRequestService>()
        val controller = Erc20SendRequestController(service)

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
