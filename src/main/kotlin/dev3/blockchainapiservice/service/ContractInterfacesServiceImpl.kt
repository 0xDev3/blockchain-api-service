package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractInterfacesRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.util.InterfaceId
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ContractInterfacesServiceImpl(
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository,
    private val contractInterfacesRepository: ContractInterfacesRepository,
    private val contractMetadataRepository: ContractMetadataRepository
) : ContractInterfacesService {

    companion object : KLogging()

    override fun attachMatchingInterfacesToDecorator(contractDecorator: ContractDecorator): ContractDecorator {
        val matchingInterfaces = contractInterfacesRepository.getAllWithPartiallyMatchingInterfaces(
            abiFunctionSignatures = contractDecorator.functions.map { it.signature }.toSet(),
            abiEventSignatures = contractDecorator.events.map { it.signature }.toSet()
        )
            .filterNot { contractDecorator.implements.contains(it.id) }
            .filter { it.functionDecorators.isNotEmpty() }
            .map { it.id.value }
            .toSet()

        val newManifest = contractDecorator.manifest.copy(
            implements = contractDecorator.manifest.implements + matchingInterfaces
        )

        return ContractDecorator(
            id = contractDecorator.id,
            artifact = contractDecorator.artifact,
            manifest = newManifest,
            imported = true,
            interfacesProvider = contractInterfacesRepository::getById
        )
    }

    override fun getSuggestedInterfacesForImportedSmartContract(id: UUID): List<InterfaceManifestJsonWithId> {
        logger.debug { "Fetching suggested interface for contract with id: $id" }

        val contractDeploymentRequest = contractDeploymentRequestRepository.getById(id)?.takeIf { it.imported }
            ?: throw ResourceNotFoundException("Imported contract deployment request not found for ID: $id")

        val importedManifest = importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(
            contractId = contractDeploymentRequest.contractId,
            projectId = contractDeploymentRequest.projectId
        ) ?: throw ResourceNotFoundException(
            "Imported contract decorator not found for contract ID: ${contractDeploymentRequest.contractId}" +
                " and project ID: ${contractDeploymentRequest.projectId}"
        )

        return contractInterfacesRepository.getAllWithPartiallyMatchingInterfaces(
            abiFunctionSignatures = importedManifest.functionDecorators.map { it.signature }.toSet(),
            abiEventSignatures = importedManifest.eventDecorators.map { it.signature }.toSet()
        ).filterNot { importedManifest.implements.contains(it.id.value) }
    }

    override fun addInterfacesToImportedContract(
        importedContractId: UUID,
        projectId: UUID,
        interfaces: List<InterfaceId>
    ) {
        logger.info {
            "Add interface to imported contract decorator, importedContractId: $importedContractId," +
                " projectId: $projectId, interfaces: $interfaces"
        }

        updateInterfaces(importedContractId, projectId) { it + interfaces.map { i -> i.value } }
    }

    override fun removeInterfacesFromImportedContract(
        importedContractId: UUID,
        projectId: UUID,
        interfaces: List<InterfaceId>
    ) {
        logger.info {
            "Remove interface from imported contract decorator, importedContractId: $importedContractId," +
                " projectId: $projectId, interfaceId: $interfaces"
        }

        updateInterfaces(importedContractId, projectId) { it - interfaces.map { i -> i.value } }
    }

    override fun setImportedContractInterfaces(
        importedContractId: UUID,
        projectId: UUID,
        interfaces: List<InterfaceId>
    ) {
        logger.info {
            "Set imported contract decorator interfaces, importedContractId: $importedContractId," +
                " projectId: $projectId, interfaces: $interfaces"
        }

        updateInterfaces(importedContractId, projectId) { interfaces.map { it.value }.toSet() }
    }

    private fun updateInterfaces(
        importedContractId: UUID,
        projectId: UUID,
        interfacesProvider: (Set<String>) -> Set<String>
    ) {
        val contractDeploymentRequest = contractDeploymentRequestRepository.getById(importedContractId)
            ?.takeIf { it.imported && it.projectId == projectId }
            ?: throw ResourceNotFoundException(
                "Imported contract deployment request not found for ID: $importedContractId"
            )

        val importedManifest = importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(
            contractId = contractDeploymentRequest.contractId,
            projectId = contractDeploymentRequest.projectId
        ) ?: throw ResourceNotFoundException(
            "Imported contract decorator not found for contract ID: ${contractDeploymentRequest.contractId}" +
                " and project ID: ${contractDeploymentRequest.projectId}"
        )

        val newInterfaces = interfacesProvider(importedManifest.implements)
        val newManifest = importedManifest.copy(implements = newInterfaces)

        val importedArtifact = importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(
            contractId = contractDeploymentRequest.contractId,
            projectId = contractDeploymentRequest.projectId
        )!!

        ContractDecorator(
            id = contractDeploymentRequest.contractId,
            artifact = importedArtifact,
            manifest = newManifest,
            imported = true,
            interfacesProvider = contractInterfacesRepository::getById
        )

        val newInterfacesList = newInterfaces.map { InterfaceId(it) }.toList()

        importedContractDecoratorRepository.updateInterfaces(
            contractId = contractDeploymentRequest.contractId,
            projectId = projectId,
            interfaces = newInterfacesList,
            manifest = newManifest
        )

        contractMetadataRepository.updateInterfaces(
            contractId = contractDeploymentRequest.contractId,
            projectId = projectId,
            interfaces = newInterfacesList
        )
    }
}
