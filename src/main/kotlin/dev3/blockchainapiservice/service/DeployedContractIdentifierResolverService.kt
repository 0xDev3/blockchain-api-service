package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.model.params.DeployedContractIdentifier
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.ContractAddress

interface DeployedContractIdentifierResolverService {
    fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        project: Project
    ): Pair<ContractDeploymentRequestId?, ContractAddress>
}
