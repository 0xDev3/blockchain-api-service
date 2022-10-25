package dev3.blockchainapiservice.model.filters

import dev3.blockchainapiservice.util.ContractAddress
import java.util.UUID

data class ContractFunctionCallRequestFilters(
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress?
)
