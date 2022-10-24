package com.ampnet.blockchainapiservice.model.filters

import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

data class ContractFunctionCallRequestFilters(
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress?
)
