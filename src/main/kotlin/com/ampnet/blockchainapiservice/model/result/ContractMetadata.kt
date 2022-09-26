package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import java.util.UUID

data class ContractMetadata(
    val id: UUID,
    val name: String?,
    val description: String?,
    val contractId: ContractId,
    val contractTags: List<ContractTag>,
    val contractImplements: List<ContractTrait>,
    val projectId: UUID
)
