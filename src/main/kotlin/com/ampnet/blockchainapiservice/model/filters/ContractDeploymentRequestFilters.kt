package com.ampnet.blockchainapiservice.model.filters

import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.InterfaceId

data class ContractDeploymentRequestFilters(
    val contractIds: OrList<ContractId>,
    val contractTags: OrList<AndList<ContractTag>>,
    val contractImplements: OrList<AndList<InterfaceId>>,
    val deployedOnly: Boolean
)
