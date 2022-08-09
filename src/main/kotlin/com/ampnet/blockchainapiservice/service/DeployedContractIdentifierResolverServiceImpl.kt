package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.ContractNotYetDeployedException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.DeployedContractAddressIdentifier
import com.ampnet.blockchainapiservice.model.params.DeployedContractAliasIdentifier
import com.ampnet.blockchainapiservice.model.params.DeployedContractIdIdentifier
import com.ampnet.blockchainapiservice.model.params.DeployedContractIdentifier
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DeployedContractIdentifierResolverServiceImpl(
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository
) : DeployedContractIdentifierResolverService {

    companion object : KLogging()

    override fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        projectId: UUID
    ): Pair<UUID?, ContractAddress> =
        when (identifier) {
            is DeployedContractIdIdentifier -> {
                logger.info { "Fetching deployed contract by id: ${identifier.id}" }
                contractDeploymentRequestRepository.getById(identifier.id)
                    ?.deployedContractIdAndAddress()
                    ?: throw ResourceNotFoundException("Deployed contract not found for ID: ${identifier.id}")
            }

            is DeployedContractAliasIdentifier -> {
                logger.info { "Fetching deployed contract by id: ${identifier.alias}, projectId: $projectId" }
                contractDeploymentRequestRepository.getByAliasAndProjectId(identifier.alias, projectId)
                    ?.deployedContractIdAndAddress()
                    ?: throw ResourceNotFoundException("Deployed contract not found for alias: ${identifier.alias}")
            }

            is DeployedContractAddressIdentifier -> {
                logger.info { "Using contract address for function call: ${identifier.contractAddress}" }
                Pair<UUID?, ContractAddress>(null, identifier.contractAddress)
            }
        }

    private fun ContractDeploymentRequest.deployedContractIdAndAddress(): Pair<UUID?, ContractAddress> =
        Pair(id, contractAddress ?: throw ContractNotYetDeployedException(id, alias))
}
