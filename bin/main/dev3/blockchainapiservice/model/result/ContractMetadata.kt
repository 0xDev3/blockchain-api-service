package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId
import java.util.UUID

data class ContractMetadata(
    val id: UUID,
    val name: String?,
    val description: String?,
    val contractId: ContractId,
    val contractTags: List<ContractTag>,
    val contractImplements: List<InterfaceId>,
    val projectId: UUID
)
