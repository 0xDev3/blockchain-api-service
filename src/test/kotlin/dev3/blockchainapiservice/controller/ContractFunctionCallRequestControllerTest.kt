package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.features.contract.deployment.model.params.DeployedContractIdIdentifier
import dev3.blockchainapiservice.features.contract.functioncall.controller.ContractFunctionCallRequestController
import dev3.blockchainapiservice.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import dev3.blockchainapiservice.features.contract.functioncall.model.params.CreateContractFunctionCallRequestParams
import dev3.blockchainapiservice.features.contract.functioncall.model.request.CreateContractFunctionCallRequest
import dev3.blockchainapiservice.features.contract.functioncall.model.response.ContractFunctionCallRequestResponse
import dev3.blockchainapiservice.features.contract.functioncall.model.response.ContractFunctionCallRequestsResponse
import dev3.blockchainapiservice.features.contract.functioncall.model.result.ContractFunctionCallRequest
import dev3.blockchainapiservice.features.contract.functioncall.service.ContractFunctionCallRequestService
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.request.AttachTransactionInfoRequest
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionAndFunctionData
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class ContractFunctionCallRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequest() {
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val params = CreateContractFunctionCallRequestParams(
            identifier = DeployedContractIdIdentifier(deployedContractId),
            functionName = "test",
            functionParams = emptyList(),
            ethAmount = Balance(BigInteger.TEN),
            redirectUrl = "redirect-url",
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            callerAddress = WalletAddress("a")
        )
        val result = WithFunctionData(
            value = ContractFunctionCallRequest(
                id = ContractFunctionCallRequestId(UUID.randomUUID()),
                deployedContractId = deployedContractId,
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = ProjectId(UUID.randomUUID()),
                createdAt = TestData.TIMESTAMP,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                contractAddress = ContractAddress("cafebabe"),
                txHash = TransactionHash("tx-hash"),
                functionName = "test",
                functionParams = TestData.EMPTY_JSON_ARRAY,
                ethAmount = Balance(BigInteger.TEN),
                callerAddress = WalletAddress("a")
            ),
            data = FunctionData("00")
        )
        val project = Project(
            id = result.value.projectId,
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("b"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = result.value.chainId,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<ContractFunctionCallRequestService>()

        suppose("contract function call request will be created") {
            call(service.createContractFunctionCallRequest(params, project))
                .willReturn(result)
        }

        val controller = ContractFunctionCallRequestController(service)

        verify("controller returns correct response") {
            val request = CreateContractFunctionCallRequest(
                deployedContractId = deployedContractId,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = params.functionName,
                functionParams = params.functionParams,
                ethAmount = params.ethAmount.rawValue,
                redirectUrl = params.redirectUrl,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig,
                callerAddress = params.callerAddress?.rawValue
            )
            val response = controller.createContractFunctionCallRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractFunctionCallRequestResponse(
                            id = result.value.id,
                            status = Status.PENDING,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress.rawValue,
                            deployedContractId = result.value.deployedContractId,
                            functionName = result.value.functionName,
                            functionParams = TestData.EMPTY_JSON_ARRAY,
                            functionCallData = result.data.value,
                            ethAmount = result.value.ethAmount.rawValue,
                            callerAddress = result.value.callerAddress?.rawValue,
                            functionCallTx = TransactionResponse(
                                txHash = null,
                                from = result.value.callerAddress?.rawValue,
                                to = result.value.contractAddress.rawValue,
                                data = result.data.value,
                                value = result.value.ethAmount.rawValue,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            events = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequest() {
        val id = ContractFunctionCallRequestId(UUID.randomUUID())
        val service = mock<ContractFunctionCallRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionAndFunctionData(
            value = ContractFunctionCallRequest(
                id = id,
                deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = ProjectId(UUID.randomUUID()),
                createdAt = TestData.TIMESTAMP,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                contractAddress = ContractAddress("cafebabe"),
                txHash = txHash,
                functionName = "test",
                functionParams = TestData.EMPTY_JSON_ARRAY,
                ethAmount = Balance(BigInteger.TEN),
                callerAddress = WalletAddress("a")
            ),
            functionData = FunctionData("00"),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("cafebabe"),
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some contract function call request will be fetched") {
            call(service.getContractFunctionCallRequest(id))
                .willReturn(result)
        }

        val controller = ContractFunctionCallRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractFunctionCallRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractFunctionCallRequestResponse(
                            id = result.value.id,
                            status = result.status,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress.rawValue,
                            deployedContractId = result.value.deployedContractId,
                            functionName = result.value.functionName,
                            functionParams = TestData.EMPTY_JSON_ARRAY,
                            functionCallData = result.functionData.value,
                            ethAmount = result.value.ethAmount.rawValue,
                            callerAddress = result.value.callerAddress?.rawValue,
                            functionCallTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = result.transactionData.value.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            ),
                            events = emptyList()
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestsByProjectIdAndFilters() {
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<ContractFunctionCallRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionAndFunctionData(
            value = ContractFunctionCallRequest(
                id = ContractFunctionCallRequestId(UUID.randomUUID()),
                deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = ProjectId(UUID.randomUUID()),
                createdAt = TestData.TIMESTAMP,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                contractAddress = ContractAddress("cafebabe"),
                txHash = txHash,
                functionName = "test",
                functionParams = TestData.EMPTY_JSON_ARRAY,
                ethAmount = Balance(BigInteger.TEN),
                callerAddress = WalletAddress("a")
            ),
            functionData = FunctionData("00"),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("cafebabe"),
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        val filters = ContractFunctionCallRequestFilters(
            deployedContractId = result.value.deployedContractId,
            contractAddress = result.value.contractAddress
        )

        suppose("some contract function call requests will be fetched by project ID and filters") {
            call(service.getContractFunctionCallRequestsByProjectIdAndFilters(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractFunctionCallRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractFunctionCallRequestsByProjectIdAndFilters(
                projectId = projectId,
                deployedContractId = filters.deployedContractId,
                contractAddress = filters.contractAddress?.rawValue
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractFunctionCallRequestsResponse(
                            listOf(
                                ContractFunctionCallRequestResponse(
                                    id = result.value.id,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    redirectUrl = result.value.redirectUrl,
                                    projectId = result.value.projectId,
                                    createdAt = result.value.createdAt.value,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig.orEmpty(),
                                    contractAddress = result.value.contractAddress.rawValue,
                                    deployedContractId = result.value.deployedContractId,
                                    functionName = result.value.functionName,
                                    functionParams = TestData.EMPTY_JSON_ARRAY,
                                    functionCallData = result.functionData.value,
                                    ethAmount = result.value.ethAmount.rawValue,
                                    callerAddress = result.value.callerAddress?.rawValue,
                                    functionCallTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = result.transactionData.value.rawValue,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = result.transactionData.timestamp?.value
                                    ),
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
        val service = mock<ContractFunctionCallRequestService>()
        val controller = ContractFunctionCallRequestController(service)

        val id = ContractFunctionCallRequestId(UUID.randomUUID())
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
