package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.params.DeployedContractIdentifier
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface DeployedContractIdentifierResolverService {
    fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        project: Project
    ): Pair<UUID?, ContractAddress>
}
