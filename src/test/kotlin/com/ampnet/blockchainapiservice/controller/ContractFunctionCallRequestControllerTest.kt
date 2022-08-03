package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import com.ampnet.blockchainapiservice.model.params.CreateContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.params.DeployedContractIdIdentifier
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateContractFunctionCallRequest
import com.ampnet.blockchainapiservice.model.response.ContractFunctionCallRequestResponse
import com.ampnet.blockchainapiservice.model.response.ContractFunctionCallRequestsResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.ContractFunctionCallRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.ContractFunctionCallRequestService
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionAndFunctionData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class ContractFunctionCallRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequest() {
        val deployedContractId = UUID.randomUUID()
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
                id = UUID.randomUUID(),
                deployedContractId = deployedContractId,
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = UUID.randomUUID(),
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
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("b"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = result.value.chainId,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<ContractFunctionCallRequestService>()

        suppose("contract function call request will be created") {
            given(service.createContractFunctionCallRequest(params, project))
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

            assertThat(response).withMessage()
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
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequest() {
        val id = UUID.randomUUID()
        val service = mock<ContractFunctionCallRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionAndFunctionData(
            value = ContractFunctionCallRequest(
                id = id,
                deployedContractId = UUID.randomUUID(),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = UUID.randomUUID(),
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
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some contract function call request will be fetched") {
            given(service.getContractFunctionCallRequest(id))
                .willReturn(result)
        }

        val controller = ContractFunctionCallRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractFunctionCallRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
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
                                value = result.transactionData.value?.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestsByProjectIdAndFilters() {
        val projectId = UUID.randomUUID()
        val service = mock<ContractFunctionCallRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionAndFunctionData(
            value = ContractFunctionCallRequest(
                id = UUID.randomUUID(),
                deployedContractId = UUID.randomUUID(),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = UUID.randomUUID(),
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
                timestamp = TestData.TIMESTAMP
            )
        )

        val filters = ContractFunctionCallRequestFilters(
            deployedContractId = result.value.deployedContractId,
            contractAddress = result.value.contractAddress
        )

        suppose("some contract function call requests will be fetched by project ID and filters") {
            given(service.getContractFunctionCallRequestsByProjectIdAndFilters(projectId, filters))
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

            assertThat(response).withMessage()
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
                                        value = result.transactionData.value?.rawValue,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = result.transactionData.timestamp?.value
                                    )
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
