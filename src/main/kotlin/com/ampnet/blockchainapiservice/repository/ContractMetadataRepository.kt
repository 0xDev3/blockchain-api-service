package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import java.util.UUID

interface ContractMetadataRepository {
    fun createOrUpdate(
        id: UUID,
        name: String?,
        description: String?,
        contractId: ContractId,
        contractTags: List<ContractTag>,
        contractImplements: List<ContractTrait>
    ): Boolean

    fun exists(contractId: ContractId): Boolean
}
