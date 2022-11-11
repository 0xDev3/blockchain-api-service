package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.model.params.ImportContractParams
import dev3.blockchainapiservice.model.request.ImportContractRequest
import dev3.blockchainapiservice.model.request.ImportedContractInterfacesRequest
import dev3.blockchainapiservice.model.response.ContractDeploymentRequestResponse
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestResponse
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestsResponse
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.ContractDeploymentRequestService
import dev3.blockchainapiservice.service.ContractImportService
import dev3.blockchainapiservice.service.ContractInterfacesService
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.InterfaceId
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithTransactionData
import dev3.blockchainapiservice.util.ZeroAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class ImportContractControllerTest : TestBase() {

    @Test
    fun mustCorrectlyImportContract() {
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = UUID.randomUUID(),
                alias = "alias",
                name = "name",
                description = "description",
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(InterfaceId("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
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
                deployerAddress = WalletAddress("a"),
                txHash = txHash,
                imported = false,
                proxy = false,
                implementationContractAddress = null
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

        val importService = mock<ContractImportService>()
        val params = ImportContractParams(
            alias = result.value.alias,
            contractId = result.value.contractId,
            contractAddress = result.value.contractAddress!!,
            redirectUrl = result.value.redirectUrl,
            arbitraryData = result.value.arbitraryData,
            screenConfig = result.value.screenConfig
        )
        val projectId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("abc123"),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )

        suppose("some smart contract is not already imported") {
            given(importService.importExistingContract(params, project))
                .willReturn(null)
        }

        suppose("some smart contract will be imported") {
            given(importService.importContract(params, project))
                .willReturn(result.value.id)
        }

        val deploymentService = mock<ContractDeploymentRequestService>()

        suppose("imported contract will be deployed") {
            given(deploymentService.getContractDeploymentRequest(result.value.id))
                .willReturn(result)
        }

        val controller = ImportContractController(importService, deploymentService, mock())

        verify("controller returns correct response") {
            val request = ImportContractRequest(
                alias = result.value.alias,
                contractId = result.value.contractId.value,
                contractAddress = result.value.contractAddress!!.rawValue,
                redirectUrl = result.value.redirectUrl,
                arbitraryData = result.value.arbitraryData,
                screenConfig = result.value.screenConfig
            )
            val response = controller.importSmartContract(
                project = project,
                requestBody = request
            )

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.value.id,
                            alias = result.value.alias,
                            name = result.value.name,
                            description = result.value.description,
                            status = result.status,
                            contractId = result.value.contractId.value,
                            contractDeploymentData = result.value.contractData.withPrefix,
                            constructorParams = TestData.EMPTY_JSON_ARRAY,
                            contractTags = result.value.contractTags.map { it.value },
                            contractImplements = result.value.contractImplements.map { it.value },
                            initialEthAmount = result.value.initialEthAmount.rawValue,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress?.rawValue,
                            deployerAddress = result.value.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = result.transactionData.value.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            ),
                            imported = result.value.imported,
                            proxy = false,
                            implementationContractAddress = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlySuggestInterfacesForSmartContract() {
        val id = UUID.randomUUID()
        val result = InterfaceManifestJsonWithId(
            id = InterfaceId("interface-id"),
            name = "name",
            description = "description",
            tags = emptySet(),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val contractInterfacesService = mock<ContractInterfacesService>()

        suppose("some interfaces are suggested") {
            given(contractInterfacesService.getSuggestedInterfacesForImportedSmartContract(id))
                .willReturn(listOf(result))
        }

        val controller = ImportContractController(mock(), mock(), contractInterfacesService)

        verify("controller returns correct response") {
            val response = controller.getSuggestedInterfacesForImportedSmartContract(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractInterfaceManifestsResponse(
                            listOf(
                                ContractInterfaceManifestResponse(result)
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAddInterfacesToImportedContract() {
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = UUID.randomUUID(),
                alias = "alias",
                name = "name",
                description = "description",
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(InterfaceId("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
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
                deployerAddress = WalletAddress("a"),
                txHash = txHash,
                imported = false,
                proxy = false,
                implementationContractAddress = null
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
        val project = Project(
            id = result.value.projectId,
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("abc123"),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )

        val deploymentService = mock<ContractDeploymentRequestService>()

        suppose("imported contract will be returned") {
            given(deploymentService.getContractDeploymentRequest(result.value.id))
                .willReturn(result)
        }

        val contractInterfacesService = mock<ContractInterfacesService>()
        val newInterfaces = listOf(InterfaceId("new-interface"))

        val controller = ImportContractController(mock(), deploymentService, contractInterfacesService)

        verify("controller returns correct response") {
            val request = ImportedContractInterfacesRequest(newInterfaces.map { it.value })
            val response = controller.addInterfacesToImportedSmartContract(project, result.value.id, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.value.id,
                            alias = result.value.alias,
                            name = result.value.name,
                            description = result.value.description,
                            status = result.status,
                            contractId = result.value.contractId.value,
                            contractDeploymentData = result.value.contractData.withPrefix,
                            constructorParams = TestData.EMPTY_JSON_ARRAY,
                            contractTags = result.value.contractTags.map { it.value },
                            contractImplements = result.value.contractImplements.map { it.value },
                            initialEthAmount = result.value.initialEthAmount.rawValue,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress?.rawValue,
                            deployerAddress = result.value.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = result.transactionData.value.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            ),
                            imported = result.value.imported,
                            proxy = false,
                            implementationContractAddress = null
                        )
                    )
                )
        }

        verify("some interfaces are added") {
            verifyMock(contractInterfacesService)
                .addInterfacesToImportedContract(result.value.id, result.value.projectId, newInterfaces)
        }
    }

    @Test
    fun mustCorrectlyRemoveInterfacesFromImportedContract() {
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = UUID.randomUUID(),
                alias = "alias",
                name = "name",
                description = "description",
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(InterfaceId("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
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
                deployerAddress = WalletAddress("a"),
                txHash = txHash,
                imported = false,
                proxy = false,
                implementationContractAddress = null
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
        val project = Project(
            id = result.value.projectId,
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("abc123"),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )

        val deploymentService = mock<ContractDeploymentRequestService>()

        suppose("imported contract will be returned") {
            given(deploymentService.getContractDeploymentRequest(result.value.id))
                .willReturn(result)
        }

        val contractInterfacesService = mock<ContractInterfacesService>()
        val interfacesToRemove = listOf(InterfaceId("new-interface"))

        val controller = ImportContractController(mock(), deploymentService, contractInterfacesService)

        verify("controller returns correct response") {
            val request = ImportedContractInterfacesRequest(interfacesToRemove.map { it.value })
            val response = controller.removeInterfacesFromImportedSmartContract(project, result.value.id, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.value.id,
                            alias = result.value.alias,
                            name = result.value.name,
                            description = result.value.description,
                            status = result.status,
                            contractId = result.value.contractId.value,
                            contractDeploymentData = result.value.contractData.withPrefix,
                            constructorParams = TestData.EMPTY_JSON_ARRAY,
                            contractTags = result.value.contractTags.map { it.value },
                            contractImplements = result.value.contractImplements.map { it.value },
                            initialEthAmount = result.value.initialEthAmount.rawValue,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress?.rawValue,
                            deployerAddress = result.value.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = result.transactionData.value.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            ),
                            imported = result.value.imported,
                            proxy = false,
                            implementationContractAddress = null
                        )
                    )
                )
        }

        verify("some interfaces are removed") {
            verifyMock(contractInterfacesService)
                .removeInterfacesFromImportedContract(result.value.id, result.value.projectId, interfacesToRemove)
        }
    }

    @Test
    fun mustCorrectlySetInterfacesForImportedContract() {
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = UUID.randomUUID(),
                alias = "alias",
                name = "name",
                description = "description",
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(InterfaceId("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
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
                deployerAddress = WalletAddress("a"),
                txHash = txHash,
                imported = false,
                proxy = false,
                implementationContractAddress = null
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
        val project = Project(
            id = result.value.projectId,
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("abc123"),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )

        val deploymentService = mock<ContractDeploymentRequestService>()

        suppose("imported contract will be returned") {
            given(deploymentService.getContractDeploymentRequest(result.value.id))
                .willReturn(result)
        }

        val contractInterfacesService = mock<ContractInterfacesService>()
        val interfacesToSet = listOf(InterfaceId("new-interface"))

        val controller = ImportContractController(mock(), deploymentService, contractInterfacesService)

        verify("controller returns correct response") {
            val request = ImportedContractInterfacesRequest(interfacesToSet.map { it.value })
            val response = controller.setInterfacesForImportedSmartContract(project, result.value.id, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.value.id,
                            alias = result.value.alias,
                            name = result.value.name,
                            description = result.value.description,
                            status = result.status,
                            contractId = result.value.contractId.value,
                            contractDeploymentData = result.value.contractData.withPrefix,
                            constructorParams = TestData.EMPTY_JSON_ARRAY,
                            contractTags = result.value.contractTags.map { it.value },
                            contractImplements = result.value.contractImplements.map { it.value },
                            initialEthAmount = result.value.initialEthAmount.rawValue,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress?.rawValue,
                            deployerAddress = result.value.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = result.transactionData.value.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            ),
                            imported = result.value.imported,
                            proxy = false,
                            implementationContractAddress = null
                        )
                    )
                )
        }

        verify("some interfaces are set") {
            verifyMock(contractInterfacesService)
                .setImportedContractInterfaces(result.value.id, result.value.projectId, interfacesToSet)
        }
    }
}
