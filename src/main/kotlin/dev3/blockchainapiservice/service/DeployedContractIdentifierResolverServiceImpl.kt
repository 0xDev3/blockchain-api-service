package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.exception.ContractNotYetDeployedException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.features.contract.deployment.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.features.contract.deployment.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.model.params.DeployedContractAddressIdentifier
import dev3.blockchainapiservice.model.params.DeployedContractAliasIdentifier
import dev3.blockchainapiservice.model.params.DeployedContractIdIdentifier
import dev3.blockchainapiservice.model.params.DeployedContractIdentifier
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.springframework.stereotype.Service

private typealias IdAndAddress = Pair<ContractDeploymentRequestId?, ContractAddress>

@Service
class DeployedContractIdentifierResolverServiceImpl(
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val ethCommonService: EthCommonService
) : DeployedContractIdentifierResolverService {

    companion object : KLogging()

    override fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        project: Project
    ): Pair<ContractDeploymentRequestId?, ContractAddress> =
        when (identifier) {
            is DeployedContractIdIdentifier -> {
                logger.info { "Fetching deployed contract by id: ${identifier.id}" }
                contractDeploymentRequestRepository.getById(identifier.id)
                    ?.setContractAddressIfNecessary(project)
                    ?.deployedContractIdAndAddress()
                    ?: throw ResourceNotFoundException("Deployed contract not found for ID: ${identifier.id}")
            }

            is DeployedContractAliasIdentifier -> {
                logger.info { "Fetching deployed contract by id: ${identifier.alias}, projectId: ${project.id}" }
                contractDeploymentRequestRepository.getByAliasAndProjectId(identifier.alias, project.id)
                    ?.setContractAddressIfNecessary(project)
                    ?.deployedContractIdAndAddress()
                    ?: throw ResourceNotFoundException("Deployed contract not found for alias: ${identifier.alias}")
            }

            is DeployedContractAddressIdentifier -> {
                logger.info { "Using contract address for function call: ${identifier.contractAddress}" }
                val deploymentRequest = contractDeploymentRequestRepository.getByContractAddressChainIdAndProjectId(
                    contractAddress = identifier.contractAddress,
                    chainId = project.chainId,
                    projectId = project.id
                )
                Pair(deploymentRequest?.id, identifier.contractAddress)
            }
        }

    private fun ContractDeploymentRequest.deployedContractIdAndAddress(): IdAndAddress =
        Pair(id, contractAddress ?: throw ContractNotYetDeployedException(id, alias))

    private fun ContractDeploymentRequest.setContractAddressIfNecessary(project: Project): ContractDeploymentRequest =
        if (contractAddress == null) {
            ethCommonService.fetchTransactionInfo(
                txHash = txHash,
                chainId = chainId,
                customRpcUrl = project.customRpcUrl,
                events = emptyList()
            )?.deployedContractAddress?.let {
                contractDeploymentRequestRepository.setContractAddress(id, it)
                copy(contractAddress = it)
            } ?: this
        } else {
            this
        }
}
