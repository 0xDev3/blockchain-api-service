package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20SendRequest
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestResponse
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestsResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.Erc20SendRequestService
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
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
            projectId = UUID.randomUUID(),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            tokenAmount = params.tokenAmount,
            tokenSenderAddress = params.tokenSenderAddress,
            tokenRecipientAddress = params.tokenRecipientAddress,
            txHash = null,
            arbitraryData = params.arbitraryData,
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
        val data = FunctionData("data")
        val service = mock<Erc20SendRequestService>()

        suppose("ERC20 send request will be created") {
            given(service.createErc20SendRequest(params, project))
                .willReturn(WithFunctionDataOrEthValue(result, data, null))
        }

        val controller = Erc20SendRequestController(service)

        verify("controller returns correct response") {
            val request = CreateErc20SendRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress?.rawValue,
                assetType = AssetType.TOKEN,
                amount = params.tokenAmount.rawValue,
                senderAddress = params.tokenSenderAddress?.rawValue,
                recipientAddress = params.tokenRecipientAddress.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createErc20SendRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20SendRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
                            amount = result.tokenAmount.rawValue,
                            senderAddress = result.tokenSenderAddress?.rawValue,
                            recipientAddress = result.tokenRecipientAddress.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig,
                            redirectUrl = result.redirectUrl,
                            sendTx = TransactionResponse(
                                txHash = null,
                                from = result.tokenSenderAddress?.rawValue,
                                to = result.tokenAddress!!.rawValue,
                                data = data.value,
                                value = null,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            createdAt = TestData.TIMESTAMP.value
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
                projectId = UUID.randomUUID(),
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
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = null,
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
            val response = controller.getErc20SendRequest(id, rpcSpec)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20SendRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
                            status = result.status,
                            chainId = result.value.chainId.value,
                            tokenAddress = result.value.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
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
                                data = result.transactionData.data?.value,
                                value = null,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = TestData.TIMESTAMP.value
                            ),
                            createdAt = result.value.createdAt.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsByProjectId() {
        val id = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val rpcSpec = RpcUrlSpec("url", "url-override")
        val service = mock<Erc20SendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20SendRequest(
                id = id,
                projectId = projectId,
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
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = null,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some ERC20 send requests will be fetched by project ID") {
            given(service.getErc20SendRequestsByProjectId(projectId, rpcSpec))
                .willReturn(listOf(result))
        }

        val controller = Erc20SendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getErc20SendRequestsByProjectId(projectId, rpcSpec)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20SendRequestsResponse(
                            listOf(
                                Erc20SendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
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
                                        data = result.transactionData.data?.value,
                                        value = null,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsBySender() {
        val id = UUID.randomUUID()
        val sender = WalletAddress("b")
        val rpcSpec = RpcUrlSpec("url", "url-override")
        val service = mock<Erc20SendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20SendRequest(
                id = id,
                projectId = UUID.randomUUID(),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                tokenAmount = Balance(BigInteger.TEN),
                tokenSenderAddress = sender,
                tokenRecipientAddress = WalletAddress("c"),
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
                value = null,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some ERC20 send requests will be fetched by sender") {
            given(service.getErc20SendRequestsBySender(sender, rpcSpec))
                .willReturn(listOf(result))
        }

        val controller = Erc20SendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getErc20SendRequestsBySender(sender, rpcSpec)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20SendRequestsResponse(
                            listOf(
                                Erc20SendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
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
                                        data = result.transactionData.data?.value,
                                        value = null,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsByRecipient() {
        val id = UUID.randomUUID()
        val recipient = WalletAddress("c")
        val rpcSpec = RpcUrlSpec("url", "url-override")
        val service = mock<Erc20SendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20SendRequest(
                id = id,
                projectId = UUID.randomUUID(),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                tokenAmount = Balance(BigInteger.TEN),
                tokenSenderAddress = WalletAddress("b"),
                tokenRecipientAddress = recipient,
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
                value = null,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some ERC20 send requests will be fetched by recipient") {
            given(service.getErc20SendRequestsByRecipient(recipient, rpcSpec))
                .willReturn(listOf(result))
        }

        val controller = Erc20SendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getErc20SendRequestsByRecipient(recipient, rpcSpec)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20SendRequestsResponse(
                            listOf(
                                Erc20SendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
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
                                        data = result.transactionData.data?.value,
                                        value = null,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val service = mock<Erc20SendRequestService>()
        val controller = Erc20SendRequestController(service)

        val id = UUID.randomUUID()
        val txHash = "tx-hash"
        val caller = "c"

        suppose("transaction info will be attached") {
            val request = AttachTransactionInfoRequest(txHash, caller)
            controller.attachTransactionInfo(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("transaction info is correctly attached") {
            verifyMock(service)
                .attachTxInfo(id, TransactionHash(txHash), WalletAddress(caller))

            verifyNoMoreInteractions(service)
        }
    }
}
