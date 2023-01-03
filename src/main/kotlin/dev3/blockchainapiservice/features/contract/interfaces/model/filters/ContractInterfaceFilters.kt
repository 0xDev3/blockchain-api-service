package dev3.blockchainapiservice.features.contract.interfaces.model.filters

import dev3.blockchainapiservice.model.filters.AndList
import dev3.blockchainapiservice.model.filters.OrList
import dev3.blockchainapiservice.util.ContractTag

data class ContractInterfaceFilters(
    val interfaceTags: OrList<AndList<ContractTag>>,
)
