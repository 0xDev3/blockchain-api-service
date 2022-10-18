package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.repository.ContractInterfacesRepository
import com.ampnet.blockchainapiservice.repository.ImportedContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.InterfaceId
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ContractInterfacesServiceImpl(
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository,
    private val contractInterfacesRepository: ContractInterfacesRepository
) : ContractInterfacesService {

    companion object : KLogging()

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
            interfacesProvider = contractInterfacesRepository::getById
        )

        importedContractDecoratorRepository.updateInterfaces(
            contractId = contractDeploymentRequest.contractId,
            projectId = projectId,
            interfaces = newInterfaces.map { InterfaceId(it) }.toList(),
            manifest = newManifest
        )
    }
}
