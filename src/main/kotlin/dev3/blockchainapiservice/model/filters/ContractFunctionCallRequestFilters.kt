package dev3.blockchainapiservice.model.filters

import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.util.ContractAddress

data class ContractFunctionCallRequestFilters(
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress?
)
