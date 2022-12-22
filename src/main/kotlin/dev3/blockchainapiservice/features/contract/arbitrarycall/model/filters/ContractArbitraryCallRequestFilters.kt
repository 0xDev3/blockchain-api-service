package dev3.blockchainapiservice.features.contract.arbitrarycall.model.filters

import dev3.blockchainapiservice.util.ContractAddress
import java.util.UUID

data class ContractArbitraryCallRequestFilters(
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress?
)
