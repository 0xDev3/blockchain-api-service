package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachSignedMessageRequest
import com.ampnet.blockchainapiservice.model.request.CreateAssetBalanceRequest
import com.ampnet.blockchainapiservice.model.response.AssetBalanceRequestResponse
import com.ampnet.blockchainapiservice.model.response.AssetBalanceRequestsResponse
import com.ampnet.blockchainapiservice.model.response.BalanceResponse
import com.ampnet.blockchainapiservice.model.result.AssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullAssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.AssetBalanceRequestService
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
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

class AssetBalanceRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateAssetBalanceRequest() {
        val params = CreateAssetBalanceRequestParams(
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
        val result = AssetBalanceRequest(
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            blockNumber = params.blockNumber,
            requestedWalletAddress = params.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = params.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val project = Project(
            id = result.projectId,
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<AssetBalanceRequestService>()

        suppose("asset balance request will be created") {
            given(service.createAssetBalanceRequest(params, project))
                .willReturn(result)
        }

        val controller = AssetBalanceRequestController(service)

        verify("controller returns correct response") {
            val request = CreateAssetBalanceRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress?.rawValue,
                assetType = AssetType.TOKEN,
                blockNumber = params.blockNumber?.value,
                walletAddress = params.requestedWalletAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createAssetBalanceRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetBalanceRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            tokenAddress = result.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
                            blockNumber = result.blockNumber?.value,
                            walletAddress = result.requestedWalletAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig.orEmpty(),
                            balance = null,
                            messageToSign = result.messageToSign,
                            signedMessage = result.signedMessage?.value,
                            createdAt = TestData.TIMESTAMP.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequest() {
        val id = UUID.randomUUID()
        val service = mock<AssetBalanceRequestService>()
        val result = FullAssetBalanceRequest(
            id = id,
            projectId = UUID.randomUUID(),
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
            balance = AccountBalance(
                wallet = WalletAddress("def"),
                blockNumber = BlockNumber(BigInteger.TEN),
                timestamp = UtcDateTime.ofEpochSeconds(0L),
                amount = Balance(BigInteger.ONE)
            ),
            messageToSign = "message-to-sign",
            signedMessage = SignedMessage("signed-message"),
            createdAt = TestData.TIMESTAMP
        )

        suppose("some asset balance request will be fetched") {
            given(service.getAssetBalanceRequest(id))
                .willReturn(result)
        }

        val controller = AssetBalanceRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetBalanceRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetBalanceRequestResponse(
                            id = result.id,
                            projectId = result.projectId,
                            status = result.status,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            tokenAddress = result.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
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
                            signedMessage = result.signedMessage?.value,
                            createdAt = result.createdAt.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequestsByProjectId() {
        val id = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val service = mock<AssetBalanceRequestService>()
        val result = FullAssetBalanceRequest(
            id = id,
            projectId = projectId,
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
            balance = AccountBalance(
                wallet = WalletAddress("def"),
                blockNumber = BlockNumber(BigInteger.TEN),
                timestamp = UtcDateTime.ofEpochSeconds(0L),
                amount = Balance(BigInteger.ONE)
            ),
            messageToSign = "message-to-sign",
            signedMessage = SignedMessage("signed-message"),
            createdAt = TestData.TIMESTAMP
        )

        suppose("some asset balance requests will be fetched by project ID") {
            given(service.getAssetBalanceRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AssetBalanceRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetBalanceRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetBalanceRequestsResponse(
                            listOf(
                                AssetBalanceRequestResponse(
                                    id = result.id,
                                    projectId = result.projectId,
                                    status = result.status,
                                    chainId = result.chainId.value,
                                    redirectUrl = result.redirectUrl,
                                    tokenAddress = result.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
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
                                    signedMessage = result.signedMessage?.value,
                                    createdAt = result.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachSignedMessage() {
        val service = mock<AssetBalanceRequestService>()
        val controller = AssetBalanceRequestController(service)

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
