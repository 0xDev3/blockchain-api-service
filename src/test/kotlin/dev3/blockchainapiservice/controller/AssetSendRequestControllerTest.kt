package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.features.asset.send.controller.AssetSendRequestController
import dev3.blockchainapiservice.features.asset.send.model.params.CreateAssetSendRequestParams
import dev3.blockchainapiservice.features.asset.send.model.request.CreateAssetSendRequest
import dev3.blockchainapiservice.features.asset.send.model.response.AssetSendRequestResponse
import dev3.blockchainapiservice.features.asset.send.model.response.AssetSendRequestsResponse
import dev3.blockchainapiservice.features.asset.send.model.result.AssetSendRequest
import dev3.blockchainapiservice.features.asset.send.service.AssetSendRequestService
import dev3.blockchainapiservice.generated.jooq.id.AssetSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.request.AttachTransactionInfoRequest
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.AssetType
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import dev3.blockchainapiservice.util.WithTransactionData
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class AssetSendRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateAssetSendRequest() {
        val params = CreateAssetSendRequestParams(
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = AssetSendRequest(
            id = AssetSendRequestId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            assetAmount = params.assetAmount,
            assetSenderAddress = params.assetSenderAddress,
            assetRecipientAddress = params.assetRecipientAddress,
            txHash = null,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val project = Project(
            id = result.projectId,
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val data = FunctionData("data")
        val service = mock<AssetSendRequestService>()

        suppose("asset send request will be created") {
            call(service.createAssetSendRequest(params, project))
                .willReturn(WithFunctionDataOrEthValue(result, data, null))
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val request = CreateAssetSendRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress?.rawValue,
                assetType = AssetType.TOKEN,
                amount = params.assetAmount.rawValue,
                senderAddress = params.assetSenderAddress?.rawValue,
                recipientAddress = params.assetRecipientAddress.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createAssetSendRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
                            amount = result.assetAmount.rawValue,
                            senderAddress = result.assetSenderAddress?.rawValue,
                            recipientAddress = result.assetRecipientAddress.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig,
                            redirectUrl = result.redirectUrl,
                            sendTx = TransactionResponse(
                                txHash = null,
                                from = result.assetSenderAddress?.rawValue,
                                to = result.tokenAddress!!.rawValue,
                                data = data.value,
                                value = BigInteger.ZERO,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            createdAt = TestData.TIMESTAMP.value,
                            events = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequest() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val service = mock<AssetSendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = AssetSendRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                assetAmount = Balance(BigInteger.TEN),
                assetSenderAddress = WalletAddress("b"),
                assetRecipientAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset send request will be fetched") {
            call(service.getAssetSendRequest(id))
                .willReturn(result)
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetSendRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
                            status = result.status,
                            chainId = result.value.chainId.value,
                            tokenAddress = result.value.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
                            amount = result.value.assetAmount.rawValue,
                            senderAddress = result.value.assetSenderAddress?.rawValue,
                            recipientAddress = result.value.assetRecipientAddress.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig,
                            redirectUrl = result.value.redirectUrl,
                            sendTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = BigInteger.ZERO,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = TestData.TIMESTAMP.value
                            ),
                            createdAt = result.value.createdAt.value,
                            events = emptyList()
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByProjectId() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<AssetSendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = AssetSendRequest(
                id = id,
                projectId = projectId,
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                assetAmount = Balance(BigInteger.TEN),
                assetSenderAddress = WalletAddress("b"),
                assetRecipientAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset send requests will be fetched by project ID") {
            call(service.getAssetSendRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetSendRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestsResponse(
                            listOf(
                                AssetSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
                                    amount = result.value.assetAmount.rawValue,
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    recipientAddress = result.value.assetRecipientAddress.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    sendTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value,
                                    events = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsBySender() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val sender = WalletAddress("b")
        val service = mock<AssetSendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = AssetSendRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                assetAmount = Balance(BigInteger.TEN),
                assetSenderAddress = sender,
                assetRecipientAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset send requests will be fetched by sender") {
            call(service.getAssetSendRequestsBySender(sender))
                .willReturn(listOf(result))
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetSendRequestsBySender(sender.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestsResponse(
                            listOf(
                                AssetSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
                                    amount = result.value.assetAmount.rawValue,
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    recipientAddress = result.value.assetRecipientAddress.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    sendTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value,
                                    events = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByRecipient() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val recipient = WalletAddress("c")
        val service = mock<AssetSendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = AssetSendRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                assetAmount = Balance(BigInteger.TEN),
                assetSenderAddress = WalletAddress("b"),
                assetRecipientAddress = recipient,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset send requests will be fetched by recipient") {
            call(service.getAssetSendRequestsByRecipient(recipient))
                .willReturn(listOf(result))
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetSendRequestsByRecipient(recipient.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestsResponse(
                            listOf(
                                AssetSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
                                    amount = result.value.assetAmount.rawValue,
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    recipientAddress = result.value.assetRecipientAddress.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    sendTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value,
                                    events = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val service = mock<AssetSendRequestService>()
        val controller = AssetSendRequestController(service)

        val id = AssetSendRequestId(UUID.randomUUID())
        val txHash = "tx-hash"
        val caller = "c"

        suppose("transaction info will be attached") {
            val request = AttachTransactionInfoRequest(txHash, caller)
            controller.attachTransactionInfo(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("transaction info is correctly attached") {
            expectInteractions(service) {
                once.attachTxInfo(id, TransactionHash(txHash), WalletAddress(caller))
            }
        }
    }
}
