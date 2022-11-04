package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.properties.Chain
import dev3.blockchainapiservice.exception.ContractInterfaceNotFoundException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.json.AbiInputOutput
import dev3.blockchainapiservice.model.json.AbiObject
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.EventDecorator
import dev3.blockchainapiservice.model.json.FunctionDecorator
import dev3.blockchainapiservice.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractInterfacesRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.InterfaceId
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

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
            tags = emptySet(),
            implements = setOf("already-implemented"),
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
        private val ARTIFACT_JSON = ArtifactJson(
            contractName = "Name",
            sourceName = "Name.sol",
            abi = listOf(
                AbiObject(
                    anonymous = null,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "string",
                            name = "arg0",
                            type = "string",
                            indexed = null
                        )
                    ),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "Event",
                    type = "event"
                ),
                AbiObject(
                    anonymous = null,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "string",
                            name = "arg0",
                            type = "string",
                            indexed = null
                        )
                    ),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "function",
                    type = "function"
                )
            ),
            bytecode = "",
            deployedBytecode = "",
            linkReferences = null,
            deployedLinkReferences = null
        )
        private val EMPTY_CONTRACT_INTERFACE = InterfaceManifestJson(null, null, emptySet(), emptyList(), emptyList())
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
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = mock()
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
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
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
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
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
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getSuggestedInterfacesForImportedSmartContract(ID)
            }
        }
    }

    @Test
    fun mustCorrectlyAddContractInterfacesToImportedContract() {
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

        suppose("some imported contract artifact will be returned") {
            given(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some interfaces are in the contract interfaces repository") {
            given(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(EMPTY_CONTRACT_INTERFACE)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = contractMetadataRepository
        )

        val newInterface = listOf(InterfaceId("new-interface"))
        val newInterfaces = MANIFEST_JSON.implements + newInterface.map { it.value }

        suppose("interface is added") {
            service.addInterfacesToImportedContract(ID, PROJECT_ID, newInterface)
        }

        verify("interface is correctly added") {
            verifyMock(importedContractDecoratorRepository)
                .updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = newInterfaces.map { InterfaceId(it) }.toList(),
                    manifest = MANIFEST_JSON.copy(implements = newInterfaces)
                )

            verifyMock(contractMetadataRepository)
                .updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = newInterfaces.map { InterfaceId(it) }.toList()
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingInterfacesToNonExistentContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned for contract deployment request") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingInterfacesToNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(imported = false))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingInterfacesToNonOwnedImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-owned contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(projectId = UUID.randomUUID()))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingInterfacesToNonExistentImportedContractManifest() {
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
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowContractInterfaceNotFoundExceptionWhenAddingNonExistentContractInterfaces() {
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

        suppose("some imported contract artifact will be returned") {
            given(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ContractInterfaceNotFoundException is thrown") {
            assertThrows<ContractInterfaceNotFoundException>(message) {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustCorrectlyRemoveContractInterfacesFromImportedContract() {
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

        suppose("some imported contract artifact will be returned") {
            given(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some interfaces are in the contract interfaces repository") {
            given(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(EMPTY_CONTRACT_INTERFACE)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = contractMetadataRepository
        )

        suppose("interface is removed") {
            service.removeInterfacesFromImportedContract(
                importedContractId = ID,
                projectId = PROJECT_ID,
                interfaces = MANIFEST_JSON.implements.map { InterfaceId(it) }
            )
        }

        verify("interface is correctly removed") {
            verifyMock(importedContractDecoratorRepository)
                .updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = emptyList(),
                    manifest = MANIFEST_JSON.copy(implements = emptySet())
                )

            verifyMock(contractMetadataRepository)
                .updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = emptyList()
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenRemovingInterfacesFromNonExistentContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned for contract deployment request") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.removeInterfacesFromImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenRemovingInterfacesFromNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(imported = false))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.removeInterfacesFromImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenRemovingInterfacesFromNonOwnedImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-owned contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(projectId = UUID.randomUUID()))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.removeInterfacesFromImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenRemovingInterfacesFromNonExistentImportedContractManifest() {
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
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.removeInterfacesFromImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustCorrectlySetContractInterfacesForImportedContract() {
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

        suppose("some imported contract artifact will be returned") {
            given(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some interfaces are in the contract interfaces repository") {
            given(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(EMPTY_CONTRACT_INTERFACE)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = contractMetadataRepository
        )

        val newInterfaces = listOf(InterfaceId("new-interface"))

        suppose("interfaces are set") {
            service.setImportedContractInterfaces(ID, PROJECT_ID, newInterfaces)
        }

        verify("interfaces are correctly set") {
            verifyMock(importedContractDecoratorRepository)
                .updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = newInterfaces,
                    manifest = MANIFEST_JSON.copy(implements = newInterfaces.map { it.value }.toSet())
                )

            verifyMock(contractMetadataRepository)
                .updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = newInterfaces
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSettingInterfacesForNonExistentContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned for contract deployment request") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSettingInterfacesForNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(imported = false))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSettingInterfacesForNonOwnedImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-owned contract deployment request will be returned") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(projectId = UUID.randomUUID()))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSettingInterfacesForNonExistentImportedContractManifest() {
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
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowContractInterfaceNotFoundExceptionWhenSettingNonExistentContractInterfaces() {
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

        suppose("some imported contract artifact will be returned") {
            given(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ContractInterfaceNotFoundException is thrown") {
            assertThrows<ContractInterfaceNotFoundException>(message) {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }
}
