package dev3.blockchainapiservice.model.filters

import dev3.blockchainapiservice.util.ContractTag

data class ContractInterfaceFilters(
    val interfaceTags: OrList<AndList<ContractTag>>,
)
