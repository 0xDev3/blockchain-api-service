package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.params.CreateContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateContractDeploymentRequest
import com.ampnet.blockchainapiservice.model.response.ContractDeploymentRequestResponse
import com.ampnet.blockchainapiservice.model.response.ContractDeploymentRequestsResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.ContractDeploymentRequestService
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithTransactionData
import com.ampnet.blockchainapiservice.util.ZeroAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class ContractDeploymentRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateContractDeploymentRequest() {
        val params = CreateContractDeploymentRequestParams(
            contractId = ContractId("contract-id"),
            constructorParams = listOf(FunctionArgument(WalletAddress("abc"))),
            deployerAddress = WalletAddress("a"),
            initialEthAmount = Balance(BigInteger.TEN),
            redirectUrl = "redirect-url",
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = ContractDeploymentRequest(
            id = UUID.randomUUID(),
            contractId = params.contractId,
            contractData = ContractBinaryData("00"),
            contractTags = listOf(ContractTag("contract-tag")),
            contractImplements = listOf(ContractTrait("contract-trait")),
            initialEthAmount = params.initialEthAmount,
            chainId = ChainId(1337),
            redirectUrl = params.redirectUrl!!,
            projectId = UUID.randomUUID(),
            createdAt = TestData.TIMESTAMP,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            contractAddress = ContractAddress("cafebabe"),
            deployerAddress = params.deployerAddress,
            txHash = null
        )
        val project = Project(
            id = result.projectId,
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("b"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = result.chainId,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<ContractDeploymentRequestService>()

        suppose("contract deployment request will be created") {
            given(service.createContractDeploymentRequest(params, project))
                .willReturn(result)
        }

        val controller = ContractDeploymentRequestController(service)

        verify("controller returns correct response") {
            val request = CreateContractDeploymentRequest(
                contractId = params.contractId.value,
                constructorParams = params.constructorParams,
                deployerAddress = params.deployerAddress?.rawValue,
                initialEthAmount = params.initialEthAmount.rawValue,
                redirectUrl = params.redirectUrl,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createContractDeploymentRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.id,
                            status = Status.PENDING,
                            contractId = result.contractId.value,
                            contractDeploymentData = result.contractData.withPrefix,
                            contractTags = result.contractTags.map { it.value },
                            contractImplements = result.contractImplements.map { it.value },
                            initialEthAmount = result.initialEthAmount.rawValue,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            projectId = result.projectId,
                            createdAt = result.createdAt.value,
                            arbitraryData = result.arbitraryData,
                            screenConfig = params.screenConfig,
                            contractAddress = result.contractAddress?.rawValue,
                            deployerAddress = result.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
                                txHash = null,
                                from = result.deployerAddress?.rawValue,
                                to = ZeroAddress.rawValue,
                                data = result.contractData.withPrefix,
                                value = result.initialEthAmount.rawValue,
                                blockConfirmations = null,
                                timestamp = null
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequest() {
        val id = UUID.randomUUID()
        val service = mock<ContractDeploymentRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = UUID.randomUUID(),
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(ContractTrait("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = UUID.randomUUID(),
                createdAt = TestData.TIMESTAMP,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenBeforeActionMessage = "before-action-message",
                screenAfterActionMessage = "after-action-message",
                contractAddress = ContractAddress("cafebabe"),
                deployerAddress = WalletAddress("a"),
                txHash = txHash
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ZeroAddress,
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some contract deployment will be fetched") {
            given(service.getContractDeploymentRequest(id))
                .willReturn(result)
        }

        val controller = ContractDeploymentRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractDeploymentRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.value.id,
                            status = result.status,
                            contractId = result.value.contractId.value,
                            contractDeploymentData = result.value.contractData.withPrefix,
                            contractTags = result.value.contractTags.map { it.value },
                            contractImplements = result.value.contractImplements.map { it.value },
                            initialEthAmount = result.value.initialEthAmount.rawValue,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = ScreenConfig(
                                beforeActionMessage = result.value.screenBeforeActionMessage,
                                afterActionMessage = result.value.screenAfterActionMessage
                            ).orEmpty(),
                            contractAddress = result.value.contractAddress?.rawValue,
                            deployerAddress = result.value.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
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
    fun mustCorrectlyFetchContractDeploymentRequestsByProjectIdAndFilters() {
        val projectId = UUID.randomUUID()
        val service = mock<ContractDeploymentRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = UUID.randomUUID(),
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(ContractTrait("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = UUID.randomUUID(),
                createdAt = TestData.TIMESTAMP,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenBeforeActionMessage = "before-action-message",
                screenAfterActionMessage = "after-action-message",
                contractAddress = ContractAddress("cafebabe"),
                deployerAddress = WalletAddress("a"),
                txHash = txHash
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ZeroAddress,
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        val filters = ContractDeploymentRequestFilters(
            contractIds = OrList(ContractId("contract-id")),
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(ContractTrait("trait-1"), ContractTrait("trait-2"))),
            deployedOnly = true
        )

        suppose("some contract deployment requests will be fetched by project ID and filters") {
            given(service.getContractDeploymentRequestsByProjectIdAndFilters(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDeploymentRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractDeploymentRequestsByProjectIdAndFilters(
                projectId = projectId,
                contractIds = listOf("contract-id"),
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                deployedOnly = true
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestsResponse(
                            listOf(
                                ContractDeploymentRequestResponse(
                                    id = result.value.id,
                                    status = result.status,
                                    contractId = result.value.contractId.value,
                                    contractDeploymentData = result.value.contractData.withPrefix,
                                    contractTags = result.value.contractTags.map { it.value },
                                    contractImplements = result.value.contractImplements.map { it.value },
                                    initialEthAmount = result.value.initialEthAmount.rawValue,
                                    chainId = result.value.chainId.value,
                                    redirectUrl = result.value.redirectUrl,
                                    projectId = result.value.projectId,
                                    createdAt = result.value.createdAt.value,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = ScreenConfig(
                                        beforeActionMessage = result.value.screenBeforeActionMessage,
                                        afterActionMessage = result.value.screenAfterActionMessage
                                    ).orEmpty(),
                                    contractAddress = result.value.contractAddress?.rawValue,
                                    deployerAddress = result.value.deployerAddress?.rawValue,
                                    deployTx = TransactionResponse(
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
        val service = mock<ContractDeploymentRequestService>()
        val controller = ContractDeploymentRequestController(service)

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
