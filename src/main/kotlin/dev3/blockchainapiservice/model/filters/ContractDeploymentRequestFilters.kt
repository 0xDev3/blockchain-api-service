package dev3.blockchainapiservice.model.filters

import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId

data class ContractDeploymentRequestFilters(
    val contractIds: OrList<ContractId>,
    val contractTags: OrList<AndList<ContractTag>>,
    val contractImplements: OrList<AndList<InterfaceId>>,
    val deployedOnly: Boolean
)