package com.ampnet.blockchainapiservice.model.filters

import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait

data class ContractDeploymentRequestFilters(
    val contractIds: OrList<ContractId>,
    val contractTags: OrList<AndList<ContractTag>>,
    val contractImplements: OrList<AndList<ContractTrait>>,
    val deployedOnly: Boolean
)
