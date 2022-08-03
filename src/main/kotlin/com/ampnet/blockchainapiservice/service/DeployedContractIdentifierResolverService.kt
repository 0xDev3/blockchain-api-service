package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.DeployedContractIdentifier
import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface DeployedContractIdentifierResolverService {
    fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        projectId: UUID
    ): Pair<UUID?, ContractAddress>
}
