package com.ampnet.blockchainapiservice.model.filters

import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.InterfaceId

data class ContractDecoratorFilters(
    val contractTags: OrList<AndList<ContractTag>>,
    val contractImplements: OrList<AndList<InterfaceId>>
)
