package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.generated.jooq.id.AssetMultiSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.CreateAssetMultiSendRequestParams
import dev3.blockchainapiservice.model.request.AttachTransactionInfoRequest
import dev3.blockchainapiservice.model.request.CreateAssetMultiSendRequest
import dev3.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import dev3.blockchainapiservice.model.response.AssetMultiSendRequestResponse
import dev3.blockchainapiservice.model.response.AssetMultiSendRequestsResponse
import dev3.blockchainapiservice.model.response.MultiSendItemResponse
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.AssetMultiSendRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.AssetMultiSendRequestService
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
import dev3.blockchainapiservice.util.WithMultiTransactionData
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class AssetMultiSendRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequest() {
        val params = CreateAssetMultiSendRequestParams(
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            disperseContractAddress = ContractAddress("b"),
            assetAmounts = listOf(Balance(BigInteger.TEN)),
            assetRecipientAddresses = listOf(WalletAddress("c")),
            itemNames = listOf("name"),
            assetSenderAddress = WalletAddress("b"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = "approve-before-action-message",
                afterActionMessage = "approve-after-action-message"
            ),
            disperseScreenConfig = ScreenConfig(
                beforeActionMessage = "disperse-before-action-message",
                afterActionMessage = "disperse-after-action-message"
            )
        )
        val result = AssetMultiSendRequest(
            id = AssetMultiSendRequestId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            disperseContractAddress = params.disperseContractAddress,
            assetAmounts = params.assetAmounts,
            assetRecipientAddresses = params.assetRecipientAddresses,
            itemNames = params.itemNames,
            assetSenderAddress = params.assetSenderAddress,
            approveTxHash = null,
            disperseTxHash = null,
            arbitraryData = params.arbitraryData,
            approveScreenConfig = params.approveScreenConfig,
            disperseScreenConfig = params.disperseScreenConfig,
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
        val data = FunctionData("approve-data")
        val service = mock<AssetMultiSendRequestService>()

        suppose("asset multi-send request will be created") {
            call(service.createAssetMultiSendRequest(params, project))
                .willReturn(WithFunctionDataOrEthValue(result, data, null))
        }

        val controller = AssetMultiSendRequestController(service)

        verify("controller returns correct response") {
            val request = CreateAssetMultiSendRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress?.rawValue,
                disperseContractAddress = params.disperseContractAddress.rawValue,
                assetType = AssetType.TOKEN,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = params.assetRecipientAddresses[0].rawValue,
                        amount = params.assetAmounts[0].rawValue,
                        itemName = params.itemNames[0]
                    )
                ),
                senderAddress = params.assetSenderAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                approveScreenConfig = params.approveScreenConfig,
                disperseScreenConfig = params.disperseScreenConfig
            )
            val response = controller.createAssetMultiSendRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetMultiSendRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            approveStatus = Status.PENDING,
                            disperseStatus = null,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress?.rawValue,
                            disperseContractAddress = result.disperseContractAddress.rawValue,
                            assetType = AssetType.TOKEN,
                            items = listOf(
                                MultiSendItemResponse(
                                    walletAddress = result.assetRecipientAddresses[0].rawValue,
                                    amount = result.assetAmounts[0].rawValue,
                                    itemName = result.itemNames[0]
                                )
                            ),
                            senderAddress = result.assetSenderAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            approveScreenConfig = result.approveScreenConfig,
                            disperseScreenConfig = result.disperseScreenConfig,
                            redirectUrl = result.redirectUrl,
                            approveTx = TransactionResponse(
                                txHash = null,
                                from = result.assetSenderAddress?.rawValue,
                                to = result.tokenAddress!!.rawValue,
                                data = data.value,
                                value = BigInteger.ZERO,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            disperseTx = null,
                            createdAt = TestData.TIMESTAMP.value,
                            approveEvents = null,
                            disperseEvents = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequest() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val service = mock<AssetMultiSendRequestService>()
        val approveTxHash = TransactionHash("approve-tx-hash")
        val disperseTxHash = TransactionHash("disperse-tx-hash")
        val result = WithMultiTransactionData(
            value = AssetMultiSendRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                disperseContractAddress = ContractAddress("b"),
                assetAmounts = listOf(Balance(BigInteger.TEN)),
                assetRecipientAddresses = listOf(WalletAddress("c")),
                itemNames = listOf("name"),
                assetSenderAddress = WalletAddress("d"),
                approveTxHash = approveTxHash,
                disperseTxHash = disperseTxHash,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = "approve-before-action-message",
                    afterActionMessage = "approve-after-action-message"
                ),
                disperseScreenConfig = ScreenConfig(
                    beforeActionMessage = "disperse-before-action-message",
                    afterActionMessage = "disperse-after-action-message"
                ),
                createdAt = TestData.TIMESTAMP
            ),
            approveStatus = Status.SUCCESS,
            approveTransactionData = TransactionData(
                txHash = approveTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("a"),
                data = FunctionData("approve-data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            ),
            disperseStatus = Status.SUCCESS,
            disperseTransactionData = TransactionData(
                txHash = disperseTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("b"),
                data = FunctionData("disperse-data"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset multi-send request will be fetched") {
            call(service.getAssetMultiSendRequest(id))
                .willReturn(result)
        }

        val controller = AssetMultiSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetMultiSendRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetMultiSendRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
                            approveStatus = Status.SUCCESS,
                            disperseStatus = Status.SUCCESS,
                            chainId = result.value.chainId.value,
                            tokenAddress = result.value.tokenAddress?.rawValue,
                            disperseContractAddress = result.value.disperseContractAddress.rawValue,
                            assetType = AssetType.TOKEN,
                            items = listOf(
                                MultiSendItemResponse(
                                    walletAddress = result.value.assetRecipientAddresses[0].rawValue,
                                    amount = result.value.assetAmounts[0].rawValue,
                                    itemName = result.value.itemNames[0]
                                )
                            ),
                            senderAddress = result.value.assetSenderAddress?.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            approveScreenConfig = result.value.approveScreenConfig,
                            disperseScreenConfig = result.value.disperseScreenConfig,
                            redirectUrl = result.value.redirectUrl,
                            approveTx = TransactionResponse(
                                txHash = approveTxHash.value,
                                from = result.value.assetSenderAddress?.rawValue,
                                to = result.value.tokenAddress!!.rawValue,
                                data = result.approveTransactionData?.data?.value!!,
                                value = BigInteger.ZERO,
                                blockConfirmations = result.approveTransactionData?.blockConfirmations!!,
                                timestamp = result.approveTransactionData?.timestamp?.value!!
                            ),
                            disperseTx = TransactionResponse(
                                txHash = disperseTxHash.value,
                                from = result.value.assetSenderAddress?.rawValue,
                                to = result.value.disperseContractAddress.rawValue,
                                data = result.disperseTransactionData?.data?.value!!,
                                value = BigInteger.TEN,
                                blockConfirmations = result.disperseTransactionData?.blockConfirmations!!,
                                timestamp = result.disperseTransactionData?.timestamp?.value!!
                            ),
                            createdAt = TestData.TIMESTAMP.value,
                            approveEvents = emptyList(),
                            disperseEvents = emptyList()
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsByProjectId() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<AssetMultiSendRequestService>()
        val approveTxHash = TransactionHash("approve-tx-hash")
        val disperseTxHash = TransactionHash("disperse-tx-hash")
        val result = WithMultiTransactionData(
            value = AssetMultiSendRequest(
                id = id,
                projectId = projectId,
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                disperseContractAddress = ContractAddress("b"),
                assetAmounts = listOf(Balance(BigInteger.TEN)),
                assetRecipientAddresses = listOf(WalletAddress("c")),
                itemNames = listOf("name"),
                assetSenderAddress = WalletAddress("d"),
                approveTxHash = approveTxHash,
                disperseTxHash = disperseTxHash,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = "approve-before-action-message",
                    afterActionMessage = "approve-after-action-message"
                ),
                disperseScreenConfig = ScreenConfig(
                    beforeActionMessage = "disperse-before-action-message",
                    afterActionMessage = "disperse-after-action-message"
                ),
                createdAt = TestData.TIMESTAMP
            ),
            approveStatus = Status.SUCCESS,
            approveTransactionData = TransactionData(
                txHash = approveTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("a"),
                data = FunctionData("approve-data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            ),
            disperseStatus = Status.SUCCESS,
            disperseTransactionData = TransactionData(
                txHash = disperseTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("b"),
                data = FunctionData("disperse-data"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset multi-send request will be fetched") {
            call(service.getAssetMultiSendRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AssetMultiSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetMultiSendRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetMultiSendRequestsResponse(
                            listOf(
                                AssetMultiSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    approveStatus = Status.SUCCESS,
                                    disperseStatus = Status.SUCCESS,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    disperseContractAddress = result.value.disperseContractAddress.rawValue,
                                    assetType = AssetType.TOKEN,
                                    items = listOf(
                                        MultiSendItemResponse(
                                            walletAddress = result.value.assetRecipientAddresses[0].rawValue,
                                            amount = result.value.assetAmounts[0].rawValue,
                                            itemName = result.value.itemNames[0]
                                        )
                                    ),
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    approveScreenConfig = result.value.approveScreenConfig,
                                    disperseScreenConfig = result.value.disperseScreenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    approveTx = TransactionResponse(
                                        txHash = approveTxHash.value,
                                        from = result.value.assetSenderAddress?.rawValue,
                                        to = result.value.tokenAddress!!.rawValue,
                                        data = result.approveTransactionData?.data?.value!!,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.approveTransactionData?.blockConfirmations!!,
                                        timestamp = result.approveTransactionData?.timestamp?.value!!
                                    ),
                                    disperseTx = TransactionResponse(
                                        txHash = disperseTxHash.value,
                                        from = result.value.assetSenderAddress?.rawValue,
                                        to = result.value.disperseContractAddress.rawValue,
                                        data = result.disperseTransactionData?.data?.value!!,
                                        value = BigInteger.TEN,
                                        blockConfirmations = result.disperseTransactionData?.blockConfirmations!!,
                                        timestamp = result.disperseTransactionData?.timestamp?.value!!
                                    ),
                                    createdAt = TestData.TIMESTAMP.value,
                                    approveEvents = emptyList(),
                                    disperseEvents = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsBySender() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val sender = WalletAddress("d")
        val service = mock<AssetMultiSendRequestService>()
        val approveTxHash = TransactionHash("approve-tx-hash")
        val disperseTxHash = TransactionHash("disperse-tx-hash")
        val result = WithMultiTransactionData(
            value = AssetMultiSendRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                disperseContractAddress = ContractAddress("b"),
                assetAmounts = listOf(Balance(BigInteger.TEN)),
                assetRecipientAddresses = listOf(WalletAddress("c")),
                itemNames = listOf("name"),
                assetSenderAddress = sender,
                approveTxHash = approveTxHash,
                disperseTxHash = disperseTxHash,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = "approve-before-action-message",
                    afterActionMessage = "approve-after-action-message"
                ),
                disperseScreenConfig = ScreenConfig(
                    beforeActionMessage = "disperse-before-action-message",
                    afterActionMessage = "disperse-after-action-message"
                ),
                createdAt = TestData.TIMESTAMP
            ),
            approveStatus = Status.SUCCESS,
            approveTransactionData = TransactionData(
                txHash = approveTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("a"),
                data = FunctionData("approve-data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            ),
            disperseStatus = Status.SUCCESS,
            disperseTransactionData = TransactionData(
                txHash = disperseTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("b"),
                data = FunctionData("disperse-data"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset multi-send request will be fetched") {
            call(service.getAssetMultiSendRequestsBySender(sender))
                .willReturn(listOf(result))
        }

        val controller = AssetMultiSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetMultiSendRequestsBySender(sender.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetMultiSendRequestsResponse(
                            listOf(
                                AssetMultiSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    approveStatus = Status.SUCCESS,
                                    disperseStatus = Status.SUCCESS,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    disperseContractAddress = result.value.disperseContractAddress.rawValue,
                                    assetType = AssetType.TOKEN,
                                    items = listOf(
                                        MultiSendItemResponse(
                                            walletAddress = result.value.assetRecipientAddresses[0].rawValue,
                                            amount = result.value.assetAmounts[0].rawValue,
                                            itemName = result.value.itemNames[0]
                                        )
                                    ),
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    approveScreenConfig = result.value.approveScreenConfig,
                                    disperseScreenConfig = result.value.disperseScreenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    approveTx = TransactionResponse(
                                        txHash = approveTxHash.value,
                                        from = result.value.assetSenderAddress?.rawValue,
                                        to = result.value.tokenAddress!!.rawValue,
                                        data = result.approveTransactionData?.data?.value!!,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.approveTransactionData?.blockConfirmations!!,
                                        timestamp = result.approveTransactionData?.timestamp?.value!!
                                    ),
                                    disperseTx = TransactionResponse(
                                        txHash = disperseTxHash.value,
                                        from = result.value.assetSenderAddress?.rawValue,
                                        to = result.value.disperseContractAddress.rawValue,
                                        data = result.disperseTransactionData?.data?.value!!,
                                        value = BigInteger.TEN,
                                        blockConfirmations = result.disperseTransactionData?.blockConfirmations!!,
                                        timestamp = result.disperseTransactionData?.timestamp?.value!!
                                    ),
                                    createdAt = TestData.TIMESTAMP.value,
                                    approveEvents = emptyList(),
                                    disperseEvents = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachApproveTransactionInfo() {
        val service = mock<AssetMultiSendRequestService>()
        val controller = AssetMultiSendRequestController(service)

        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val txHash = "approve-tx-hash"
        val caller = "c"

        suppose("approve transaction info will be attached") {
            val request = AttachTransactionInfoRequest(txHash, caller)
            controller.attachApproveTransactionInfo(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("transaction info is correctly attached") {
            expectInteractions(service) {
                once.attachApproveTxInfo(id, TransactionHash(txHash), WalletAddress(caller))
            }
        }
    }

    @Test
    fun mustCorrectlyAttachDisperseTransactionInfo() {
        val service = mock<AssetMultiSendRequestService>()
        val controller = AssetMultiSendRequestController(service)

        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val txHash = "disperse-tx-hash"
        val caller = "c"

        suppose("disperse transaction info will be attached") {
            val request = AttachTransactionInfoRequest(txHash, caller)
            controller.attachDisperseTransactionInfo(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("transaction info is correctly attached") {
            expectInteractions(service) {
                once.attachDisperseTxInfo(id, TransactionHash(txHash), WalletAddress(caller))
            }
        }
    }
}
