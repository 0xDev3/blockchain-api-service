package dev3.blockchainapiservice.features.contract.arbitrarycall.model.filters

import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.util.ContractAddress

data class ContractArbitraryCallRequestFilters(
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress?
)
