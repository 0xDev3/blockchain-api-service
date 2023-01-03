package dev3.blockchainapiservice.features.contract.functioncall.model.filters

import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.util.ContractAddress

data class ContractFunctionCallRequestFilters(
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress?
)
