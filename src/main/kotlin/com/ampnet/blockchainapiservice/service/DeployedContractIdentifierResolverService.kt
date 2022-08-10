package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.DeployedContractIdentifier
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface DeployedContractIdentifierResolverService {
    fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        project: Project
    ): Pair<UUID?, ContractAddress>
}
