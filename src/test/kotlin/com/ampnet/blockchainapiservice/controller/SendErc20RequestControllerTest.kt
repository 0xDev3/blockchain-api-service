package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.SendScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionHashRequest
import com.ampnet.blockchainapiservice.model.request.CreateSendErc20Request
import com.ampnet.blockchainapiservice.model.response.SendErc20RequestResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.FullSendErc20Request
import com.ampnet.blockchainapiservice.model.result.FullTransactionData
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.service.SendErc20RequestService
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class SendErc20RequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateSendErc20Request() {
        val params = CreateSendErc20RequestParams(
            clientId = "client-id",
            chainId = ChainId(123L),
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            arbitraryData = null,
            screenConfig = SendScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val result = SendErc20Request(
            id = UUID.randomUUID(),
            chainId = params.chainId!!,
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            tokenAmount = params.tokenAmount,
            tokenSenderAddress = params.tokenSenderAddress,
            tokenRecipientAddress = params.tokenRecipientAddress,
            txHash = null,
            arbitraryData = params.arbitraryData,
            sendScreenConfig = params.screenConfig
        )
        val data = FunctionData("data")
        val service = mock<SendErc20RequestService>()

        suppose("send ERC20 request will be created") {
            given(service.createSendErc20Request(params))
                .willReturn(WithFunctionData(result, data))
        }

        val controller = SendErc20RequestController(service)

        verify("controller returns correct response") {
            val response = controller.createSendErc20Request(
                CreateSendErc20Request(
                    clientId = params.clientId,
                    chainId = params.chainId?.value,
                    redirectUrl = params.redirectUrl,
                    tokenAddress = params.tokenAddress.rawValue,
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
                        SendErc20RequestResponse(
                            id = result.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress.rawValue,
                            amount = result.tokenAmount.rawValue,
                            senderAddress = result.tokenSenderAddress?.rawValue,
                            recipientAddress = result.tokenRecipientAddress.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.sendScreenConfig,
                            redirectUrl = result.redirectUrl,
                            sendTx = TransactionResponse(
                                txHash = null,
                                from = result.tokenSenderAddress?.rawValue,
                                to = result.tokenAddress.rawValue,
                                data = data.value,
                                blockConfirmations = null
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchSendErc20Request() {
        val id = UUID.randomUUID()
        val rpcSpec = RpcUrlSpec("url", "url-override")
        val service = mock<SendErc20RequestService>()
        val result = FullSendErc20Request(
            id = id,
            status = Status.SUCCESS,
            chainId = ChainId(123L),
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            arbitraryData = null,
            sendScreenConfig = SendScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            ),
            transactionData = FullTransactionData(
                txHash = TransactionHash("tx-hash"),
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                blockConfirmations = BigInteger.ONE
            )
        )

        suppose("some send ERC20 request will be fetched") {
            given(service.getSendErc20Request(id, rpcSpec))
                .willReturn(result)
        }

        val controller = SendErc20RequestController(service)

        verify("controller returns correct response") {
            assertThat(controller.getSendErc20Request(id, rpcSpec)).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        SendErc20RequestResponse(
                            id = result.id,
                            status = result.status,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress.rawValue,
                            amount = result.tokenAmount.rawValue,
                            senderAddress = result.tokenSenderAddress?.rawValue,
                            recipientAddress = result.tokenRecipientAddress.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.sendScreenConfig,
                            redirectUrl = result.redirectUrl,
                            sendTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.tokenAddress.rawValue,
                                data = result.transactionData.data.value,
                                blockConfirmations = result.transactionData.blockConfirmations
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionHash() {
        val service = mock<SendErc20RequestService>()
        val controller = SendErc20RequestController(service)

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
