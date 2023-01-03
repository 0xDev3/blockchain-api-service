package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.generated.jooq.id.ContractMetadataId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId

data class ContractMetadata(
    val id: ContractMetadataId,
    val name: String?,
    val description: String?,
    val contractId: ContractId,
    val contractTags: List<ContractTag>,
    val contractImplements: List<InterfaceId>,
    val projectId: ProjectId
)
