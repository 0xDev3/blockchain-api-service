package dev3.blockchainapiservice.features.contract.deployment.service

import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.contract.deployment.model.params.DeployedContractIdentifier
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.util.ContractAddress

interface DeployedContractIdentifierResolverService {
    fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        project: Project
    ): Pair<ContractDeploymentRequestId?, ContractAddress>
}
