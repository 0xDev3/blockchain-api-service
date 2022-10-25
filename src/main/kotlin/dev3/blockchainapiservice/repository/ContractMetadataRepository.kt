package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.result.ContractMetadata
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.InterfaceId
import java.util.UUID

interface ContractMetadataRepository {
    fun createOrUpdate(contractMetadata: ContractMetadata): Boolean
    fun updateInterfaces(contractId: ContractId, projectId: UUID, interfaces: List<InterfaceId>): Boolean
    fun exists(contractId: ContractId, projectId: UUID): Boolean
}
