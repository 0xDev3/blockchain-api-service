package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.CreateErc20LockRequestParams
import dev3.blockchainapiservice.model.request.AttachTransactionInfoRequest
import dev3.blockchainapiservice.model.request.CreateErc20LockRequest
import dev3.blockchainapiservice.model.response.Erc20LockRequestResponse
import dev3.blockchainapiservice.model.response.Erc20LockRequestsResponse
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.Erc20LockRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.Erc20LockRequestService
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.DurationSeconds
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionData
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
            projectId = UUID.randomUUID(),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            tokenAmount = params.tokenAmount,
            lockDuration = params.lockDuration,
            lockContractAddress = params.lockContractAddress,
            tokenSenderAddress = params.tokenSenderAddress,
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
        val service = mock<Erc20LockRequestService>()

        suppose("ERC20 lock request will be created") {
            given(service.createErc20LockRequest(params, project))
                .willReturn(WithFunctionData(result, data))
        }

        val controller = Erc20LockRequestController(service)

        verify("controller returns correct response") {
            val request = CreateErc20LockRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress.rawValue,
                amount = params.tokenAmount.rawValue,
                lockDurationInSeconds = params.lockDuration.rawValue,
                lockContractAddress = params.lockContractAddress.rawValue,
                senderAddress = params.tokenSenderAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createErc20LockRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20LockRequestResponse(
                            id = result.id,
                            projectId = project.id,
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
                                value = BigInteger.ZERO,
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
    fun mustCorrectlyFetchErc20LockRequest() {
        val id = UUID.randomUUID()
        val service = mock<Erc20LockRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20LockRequest(
                id = id,
                projectId = UUID.randomUUID(),
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
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some ERC20 lock request will be fetched") {
            given(service.getErc20LockRequest(id))
                .willReturn(result)
        }

        val controller = Erc20LockRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getErc20LockRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20LockRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
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
                                data = result.transactionData.data?.value,
                                value = BigInteger.ZERO,
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
    fun mustCorrectlyFetchErc20LockRequestsByProjectId() {
        val id = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val service = mock<Erc20LockRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20LockRequest(
                id = id,
                projectId = projectId,
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
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some ERC20 lock requests will be fetched by project ID") {
            given(service.getErc20LockRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = Erc20LockRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getErc20LockRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20LockRequestsResponse(
                            listOf(
                                Erc20LockRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
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
                                        data = result.transactionData.data?.value,
                                        value = BigInteger.ZERO,
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
        val service = mock<Erc20LockRequestService>()
        val controller = Erc20LockRequestController(service)

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
