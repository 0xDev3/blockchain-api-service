package com.ampnet.blockchainapiservice.model.filters

import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait

data class ContractDecoratorFilters(
    val contractTags: OrList<AndList<ContractTag>>,
    val contractImplements: OrList<AndList<ContractTrait>>
)
