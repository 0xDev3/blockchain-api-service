package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.ContractMetadata
import com.ampnet.blockchainapiservice.util.ContractId
import java.util.UUID

interface ContractMetadataRepository {
    fun createOrUpdate(contractMetadata: ContractMetadata): Boolean
    fun exists(contractId: ContractId, projectId: UUID): Boolean
}
