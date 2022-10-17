package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.repository.ContractInterfacesRepository
import com.ampnet.blockchainapiservice.repository.ImportedContractDecoratorRepository
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
}
