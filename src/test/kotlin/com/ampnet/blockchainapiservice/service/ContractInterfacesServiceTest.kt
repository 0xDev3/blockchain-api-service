package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.json.EventDecorator
import com.ampnet.blockchainapiservice.model.json.FunctionDecorator
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.repository.ContractInterfacesRepository
import com.ampnet.blockchainapiservice.repository.ImportedContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.InterfaceId
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.util.UUID

class ContractInterfacesServiceTest : TestBase() {

    companion object {
        private val ID = UUID.randomUUID()
        private val PROJECT_ID = UUID.randomUUID()
        private val CONTRACT_ID = ContractId("contract-id")
        private val CONTRACT_DEPLOYMENT_REQUEST = ContractDeploymentRequest(
            id = ID,
            alias = "alias",
            name = "name",
            description = "description",
            contractId = CONTRACT_ID,
            contractData = ContractBinaryData.invoke("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance.ZERO,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "redirect-url",
            projectId = PROJECT_ID,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            contractAddress = ContractAddress("a"),
            deployerAddress = WalletAddress("b"),
            txHash = TransactionHash("tx-hash"),
            imported = true
        )
        private val MANIFEST_JSON = ManifestJson(
            name = "name",
            description = "description",
            tags = emptyList(),
            implements = listOf("already-implemented"),
            eventDecorators = listOf(
                EventDecorator(
                    signature = "Event(string)",
                    name = "name",
                    description = "description",
                    parameterDecorators = emptyList()
                )
            ),
            constructorDecorators = emptyList(),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "function(string)",
                    name = "name",
                    description = "description",
                    parameterDecorators = emptyList(),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList()
                )
            )
        )
    }

    @Test
    fun mustCorrectlySuggestInterfacesForImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("some imported contract manifest will be returned") {
            given(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(MANIFEST_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some partially matching contract interfaces will be returned") {
            given(
                contractInterfacesRepository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = setOf("function(string)"),
                    abiEventSignatures = setOf("Event(string)")
                )
            )
                .willReturn(
                    listOf(
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("already-implemented"),
                            name = "Already Implemented",
                            description = "Already Implemented",
                            eventDecorators = MANIFEST_JSON.eventDecorators,
                            functionDecorators = emptyList()
                        ),
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("not-yet-implemented"),
                            name = "Not Yet Implemented",
                            description = "Not Yet Implemented",
                            eventDecorators = emptyList(),
                            functionDecorators = MANIFEST_JSON.functionDecorators
                        )
                    )
                )
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository
        )

        verify("correct interfaces are suggested") {
            assertThat(service.getSuggestedInterfacesForImportedSmartContract(ID)).withMessage()
                .isEqualTo(
                    listOf(
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("not-yet-implemented"),
                            name = "Not Yet Implemented",
                            description = "Not Yet Implemented",
                            eventDecorators = emptyList(),
                            functionDecorators = MANIFEST_JSON.functionDecorators
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSuggestingInterfacesForNonExistentContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned for contract deployment request") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getSuggestedInterfacesForImportedSmartContract(ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSuggestingInterfacesForNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(imported = false))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getSuggestedInterfacesForImportedSmartContract(ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSuggestingInterfacesForNonExistentImportedContractManifest() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("null will be returned for imported contract manifest") {
            given(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getSuggestedInterfacesForImportedSmartContract(ID)
            }
        }
    }
}
