package dev3.blockchainapiservice.features.contract.deployment.repository

import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.ContractMetadata
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.InterfaceId

interface ContractMetadataRepository {
    fun createOrUpdate(contractMetadata: ContractMetadata): Boolean
    fun updateInterfaces(contractId: ContractId, projectId: ProjectId, interfaces: List<InterfaceId>): Boolean
    fun exists(contractId: ContractId, projectId: ProjectId): Boolean
}
