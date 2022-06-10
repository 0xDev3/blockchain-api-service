package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachSignedMessageRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20BalanceRequest
import com.ampnet.blockchainapiservice.model.response.BalanceResponse
import com.ampnet.blockchainapiservice.model.response.Erc20BalanceRequestResponse
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullErc20BalanceRequest
import com.ampnet.blockchainapiservice.service.Erc20BalanceRequestService
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.Erc20Balance
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class Erc20BalanceRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateErc20BalanceRequest() {
        val params = CreateErc20BalanceRequestParams(
            clientId = "client-id",
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("b"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = Erc20BalanceRequest(
            id = UUID.randomUUID(),
            chainId = params.chainId!!,
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress!!,
            blockNumber = params.blockNumber,
            requestedWalletAddress = params.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = params.screenConfig
        )
        val service = mock<Erc20BalanceRequestService>()

        suppose("ERC20 balance request will be created") {
            given(service.createErc20BalanceRequest(params))
                .willReturn(result)
        }

        val controller = Erc20BalanceRequestController(service)

        verify("controller returns correct response") {
            val request = CreateErc20BalanceRequest(
                clientId = params.clientId,
                chainId = params.chainId?.value,
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress?.rawValue,
                blockNumber = params.blockNumber?.value,
                walletAddress = params.requestedWalletAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createErc20BalanceRequest(request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20BalanceRequestResponse(
                            id = result.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            tokenAddress = result.tokenAddress.rawValue,
                            blockNumber = result.blockNumber?.value,
                            walletAddress = result.requestedWalletAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig.orEmpty(),
                            balance = null,
                            messageToSign = result.messageToSign,
                            signedMessage = result.signedMessage?.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequest() {
        val id = UUID.randomUUID()
        val rpcSpec = RpcUrlSpec("url", "url-override")
        val service = mock<Erc20BalanceRequestService>()
        val result = FullErc20BalanceRequest(
            id = id,
            status = Status.SUCCESS,
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            balance = Erc20Balance(
                wallet = WalletAddress("def"),
                blockNumber = BlockNumber(BigInteger.TEN),
                timestamp = UtcDateTime.ofEpochSeconds(0L),
                amount = Balance(BigInteger.ONE)
            ),
            messageToSign = "message-to-sign",
            signedMessage = SignedMessage("signed-message")
        )

        suppose("some ERC20 balance request will be fetched") {
            given(service.getErc20BalanceRequest(id, rpcSpec))
                .willReturn(result)
        }

        val controller = Erc20BalanceRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getErc20BalanceRequest(id, rpcSpec)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20BalanceRequestResponse(
                            id = result.id,
                            status = result.status,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            tokenAddress = result.tokenAddress.rawValue,
                            blockNumber = result.blockNumber?.value,
                            walletAddress = result.requestedWalletAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig,
                            balance = result.balance?.let {
                                BalanceResponse(
                                    wallet = it.wallet.rawValue,
                                    blockNumber = it.blockNumber.value,
                                    timestamp = it.timestamp.value,
                                    amount = it.amount.rawValue
                                )
                            },
                            messageToSign = result.messageToSign,
                            signedMessage = result.signedMessage?.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachSignedMessage() {
        val service = mock<Erc20BalanceRequestService>()
        val controller = Erc20BalanceRequestController(service)

        val id = UUID.randomUUID()
        val walletAddress = WalletAddress("abc")
        val signedMessage = SignedMessage("signed-message")

        suppose("signed message will be attached") {
            val request = AttachSignedMessageRequest(walletAddress.rawValue, signedMessage.value)
            controller.attachSignedMessage(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("signed message is correctly attached") {
            verifyMock(service)
                .attachWalletAddressAndSignedMessage(id, walletAddress, signedMessage)

            verifyNoMoreInteractions(service)
        }
    }
}
