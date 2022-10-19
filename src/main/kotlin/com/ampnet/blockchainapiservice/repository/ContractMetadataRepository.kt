package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.ContractMetadata
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.InterfaceId
import java.util.UUID

interface ContractMetadataRepository {
    fun createOrUpdate(contractMetadata: ContractMetadata): Boolean
    fun updateInterfaces(contractId: ContractId, projectId: UUID, interfaces: List<InterfaceId>): Boolean
    fun exists(contractId: ContractId, projectId: UUID): Boolean
}
